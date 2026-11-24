/**
 * FOXWARE FoxyFan v3.0.4 — Native Hubitat Groovy App
 * Status: Native manual and shower control validated in v3.0.2; v3.0.4 notification identity polish
 * Baseline: user-supplied FoxyFan webCoRE v2.0.3 Build 20 (2026-11-23)
 *
 * Each installation manages ONE bathroom (one humidity sensor + one fan switch).
 * Install a second instance for a guest bathroom. All per-instance state is local.
 *
 * Operational guard: ENABLE CONTROL is OFF by default. Disable/retire the
 * matching webCoRE piston BEFORE enabling this app on the same physical fan.
 */

definition(
    name: "FOXWARE FoxyFan",
    namespace: "foxware",
    author: "FOXWARE",
    description: "Configurable humidity-triggered shower ventilation and manual fan timer",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: false,
    singleThreaded: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "FOXWARE FoxyFan v3.0.4", install: true, uninstall: true) {
        section("Instance naming") {
            // Native Hubitat app label; updates app.label and the Apps-list name.
            // The shared notificationIdentity() helper already prefers custom labels.
            label title: "Name this FoxyFan instance (e.g., Master Bathroom)", required: false
            paragraph "Give each bathroom a distinct name, such as Master Bathroom or Guest Bathroom. " +
                "This name also identifies the bathroom in push notifications."
        }
        section("Bathroom devices") {
            input name: "humiditySensor", type: "capability.relativeHumidityMeasurement",
                title: "Humidity sensor", required: true
            input name: "fanSwitch", type: "capability.switch",
                title: "Bathroom exhaust fan switch", required: true
            paragraph "Make sure the humidity device reports changes promptly (ideally every 1% RH). " +
                "Hubitat cannot infer a changing humidity value that the device does not report."
        }
        section("Shower detection") {
            input name: "riseThreshold", type: "number", title: "RH rise triggering a shower (points)",
                defaultValue: 6, range: "1..25", required: true
            input name: "historyMinutes", type: "number", title: "Rolling RH baseline window (minutes)",
                defaultValue: 5, range: "1..30", required: true
            input name: "allowedModes", type: "mode", title: "Allow automatic shower detection in these modes",
                multiple: true, required: false
            paragraph "No selected modes = allow shower detection in every location mode. " +
                "Manual ventilation remains available regardless of mode."
        }
        section("Shower dry-down and safety") {
            input name: "minimumShowerMinutes", type: "number", title: "Minimum shower fan run (minutes)",
                defaultValue: 15, range: "1..120", required: true
            input name: "dryAllowance", type: "number", title: "Drying stop threshold above original baseline (RH points)",
                defaultValue: 9, range: "0..25", required: true
            input name: "dryConfirmMinutes", type: "number", title: "Continuous drying confirmation (minutes)",
                defaultValue: 5, range: "1..60", required: true
            input name: "maximumShowerMinutes", type: "number", title: "Absolute automatic shower runtime (minutes)",
                defaultValue: 80, range: "2..240", required: true
        }
        section("Fan status synchronization") {
            input name: "postOffRefresh", type: "bool",
                title: "Refresh fan state after an app-requested OFF (driver permitting)",
                defaultValue: true, required: true
            input name: "postOnRefresh", type: "bool",
                title: "Refresh fan state after shower-start ON (driver permitting)",
                defaultValue: true, required: true
            paragraph "The app requests a delayed readback only if the expected ON/OFF report " +
                "has not arrived. Refresh is optional and does not fabricate a state change. " +
                "A shower ON request is verified after 20 seconds before trusting a cached OFF."
        }
        section("FoxFlatus manual ventilation") {
            input name: "manualMinutes", type: "number", title: "Manual fan runtime (minutes)",
                defaultValue: 20, range: "1..180", required: true
        }
        section("Notifications and diagnostics") {
            input name: "notificationDevices", type: "capability.notification",
                title: "Push notification devices (optional)", multiple: true, required: false
            input name: "notifyShower", type: "bool", title: "Notify when shower ventilation starts",
                defaultValue: true, required: true
            input name: "notifyDry", type: "bool", title: "Notify when drying finishes",
                defaultValue: true, required: true
            input name: "notifyMaximum", type: "bool", title: "Notify at automatic maximum runtime",
                defaultValue: true, required: true
            input name: "notifyManualStart", type: "bool", title: "Notify when manual FoxFlatus ventilation starts",
                defaultValue: false, required: true
            input name: "notifyManual", type: "bool", title: "Notify when manual ventilation timer completes",
                defaultValue: false, required: true
            input name: "notifyManualEarlyOff", type: "bool", title: "Notify when manual ventilation ends early",
                defaultValue: false, required: true
            input name: "staleNoticeMinutes", type: "number", title: "Warn after this long without a humidity CHANGE event (minutes)",
                defaultValue: 30, range: "5..240", required: true
            input name: "debugLogging", type: "bool", title: "Verbose debug logs", defaultValue: false
            paragraph "A static RH reading can be legitimate. Event-age warnings are informational, " +
                "not proof of a failed sensor; maximum-runtime protection remains active."
        }
        section("Activation and instance status") {
            input name: "enableControl", type: "bool", title: "Enable Fan Control (can be disabled for testing)",
                defaultValue: false, required: true
            paragraph "Instance: ${app.label ?: 'FOXWARE FoxyFan'}"
            paragraph "Notification identity: ${notificationIdentity()}"
            paragraph "Control: ${(enableControl == true) ? 'ENABLED' : 'DISABLED'}; " +
                "mode: ${state.ffMode ?: 'IDLE'}; " +
                "current RH: ${state.ffLastRh != null ? (state.ffLastRh + '%') : 'unknown'}; " +
                "shower baseline: ${state.ffShowerBase != null ? (state.ffShowerBase + '%') : 'n/a'}"
            paragraph "Last humidity-change event: ${state.ffLastHumidityEventMs ? new Date(state.ffLastHumidityEventMs as Long).toString() : 'not observed since activation'}"
            paragraph "Last transition: ${state.ffLastTransition ?: 'not recorded'}"
            if (state.ffAwaitingOff == true) {
                paragraph "WARNING: Awaiting physical fan OFF report after app shutoff. " +
                    "The app will not treat a possibly stale ON state as a new manual run."
            }
        }
    }
}

// ─── Hubitat lifecycle ──────────────────────────────────────────────────────

def installed() {
    log.info "FOXWARE FoxyFan v3.0.4 installed (control disabled by default)"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    unsubscribe()
    unschedule()

    Long t = now()
    if (state.ffMode == null) state.ffMode = "IDLE"
    if (state.ffGeneration == null) state.ffGeneration = 0
    if (state.ffEnabledAtMs == null) state.ffEnabledAtMs = t

    if (enableControl != true) {
        // Do not turn the fan OFF merely because monitoring was disabled.
        if (state.ffWasEnabled == true) {
            log.warn "FOXWARE FoxyFan: Control disabled; abandoning timers without commanding fan OFF"
        }
        resetOwnership("control disabled")
        state.ffWasEnabled = false
        return
    }

    if (!humiditySensor || !fanSwitch) {
        log.error "FOXWARE FoxyFan: Select BOTH a humidity sensor and a fan switch before enabling"
        return
    }

    // If the physical devices changed, we cannot safely continue an old run.
    String sensorId = humiditySensor.id.toString()
    String fanId = fanSwitch.id.toString()
    if ((state.ffSensorId && state.ffSensorId != sensorId) ||
        (state.ffFanId && state.ffFanId != fanId)) {
        log.warn "FOXWARE FoxyFan: Device assignment changed; old run ownership has been discarded"
        resetOwnership("device selection changed")
        state.ffRhHistory = null
        state.ffAwaitingOff = false
    }
    state.ffSensorId = sensorId
    state.ffFanId = fanId
    state.ffWasEnabled = true

    Integer window = prefInt("historyMinutes", 5, 1, 30)
    if (state.ffHistoryWindow != window) {
        state.ffRhHistory = null
        state.ffHistoryWindow = window
    }

    Double rh = readHumidity()
    if (rh != null) {
        state.ffLastRh = rh
        if (!(state.ffRhHistory instanceof List) || !state.ffRhHistory) seedHumidityHistory(rh)
    }

    subscribe(humiditySensor, "humidity", humidityChanged)
    subscribe(fanSwitch, "switch", fanSwitchChanged)
    runEvery1Minute("minuteTick")

    // Reconcile persisted runs on restart/Update before re-arming timers.
    String physical = fanValue()
    if (state.ffMode == "MANUAL" || state.ffMode == "SHOWER") {
        if (physical == "on") {
            if (state.ffMode == "SHOWER") acknowledgeShowerOn("startup current switch report")
            if (state.ffMode == "MANUAL") armManualTimeout()
        } else if (state.ffMode == "SHOWER" && state.ffAwaitingOn == true &&
                   state.ffOnRequestedAtMs != null &&
                   t - (state.ffOnRequestedAtMs as Long) < 20000L) {
            // Code was updated during the short shower ON readback window.
            scheduleShowerOnReadback()
        } else {
            resetOwnership("startup reconciliation: fan not ON")
            state.ffAwaitingOff = false
        }
    } else if (physical == "off") {
        state.ffAwaitingOff = false
    }
    if (state.ffMode == "IDLE" && physical == "on" && state.ffAwaitingOff != true) {
        // Mirrors B20's fallback discovery; start timestamp is discovery time.
        startManual("fan was ON when app initialized")
    }
    state.ffEnabledAtMs = t
    log.info "FOXWARE FoxyFan: Ready (${app.label ?: 'instance'}), mode=${state.ffMode}, fan=${physical}"
}

def uninstalled() {
    unsubscribe()
    unschedule()
    // Do not forcibly shut off any physical fan during removal.
}

// ─── Sensor and switch events ───────────────────────────────────────────────

def humidityChanged(evt) {
    if (enableControl != true) return
    Double rh = numericRh(evt?.value)
    if (rh == null) {
        log.warn "FOXWARE FoxyFan: Ignoring invalid humidity event: ${evt?.value}"
        return
    }
    state.ffLastRh = rh
    state.ffLastHumidityEventMs = now()
    if (state.ffStaleWarned == true) {
        log.info "FOXWARE FoxyFan: Humidity change reporting resumed"
        state.ffStaleWarned = false
    }

    // Automatic shower detection is only allowed while the app is IDLE;
    // B20 does not promote an already-manual ON fan into shower ownership.
    if (state.ffMode != "IDLE" || state.ffAwaitingOff == true) return
    if (fanValue() != "off" || !modeAllowsShower()) return

    List samples = state.ffRhHistory instanceof List ? state.ffRhHistory : []
    if (!samples) return
    Double base = numericRh(samples[0])
    if (base == null) return
    Double rise = rh - base
    if (rise >= prefInt("riseThreshold", 6, 1, 25)) {
        startShower(rh, base, rise)
    } else if (debugLogging == true) {
        log.debug "FOXWARE FoxyFan: Rolling RH=${rh}%, baseline=${base}%, rise=${rise} points"
    }
}

def fanSwitchChanged(evt) {
    if (enableControl != true) return
    String value = evt?.value?.toString()
    if (value == "off") {
        state.ffAwaitingOff = false
        state.ffOffWarningIssued = false
        if (state.ffMode == "MANUAL") {
            endManualEarly("fan OFF event")
        } else if (state.ffMode == "SHOWER") {
            if (state.ffAwaitingOn == true) {
                // A cached/late OFF report immediately after ON is not yet proof
                // that shower startup failed; the 20-second readback verifies it.
                if (debugLogging == true) log.debug "FOXWARE FoxyFan: Ignoring OFF while shower ON confirmation is pending"
            } else {
                resetOwnership("fan was switched OFF during shower cycle")
                log.warn "FOXWARE FoxyFan: Shower cycle interrupted by fan OFF report"
            }
        }
    } else if (value == "on") {
        if (state.ffMode == "SHOWER") {
            acknowledgeShowerOn("fan ON event")
        } else if (state.ffMode == "IDLE" && state.ffAwaitingOff != true) {
            startManual("fan ON event")
        }
        // In MANUAL, duplicate ON events must never restart the timer.
    }
}

// ─── Time-based controller ──────────────────────────────────────────────────

def minuteTick() {
    if (enableControl != true || !humiditySensor || !fanSwitch) return
    Long t = now()
    String physical = fanValue()

    // The minute watchdog can acknowledge OFF even when the OFF callback was
    // lost: a fresh currentValue('switch') == 'off' is authoritative enough.
    if (state.ffAwaitingOff == true && physical == "off") {
        state.ffAwaitingOff = false
        state.ffOffWarningIssued = false
    }

    if (state.ffMode == "SHOWER") {
        if (physical == "off") {
            if (state.ffAwaitingOn == true) {
                // Do not abandon shower ownership based on a potentially stale
                // OFF report before verifyFanAfterOn() has had its chance.
                if (state.ffOnRequestedAtMs == null ||
                    t - (state.ffOnRequestedAtMs as Long) >= 20000L) {
                    log.warn "FOXWARE FoxyFan: Shower ON still unconfirmed after 20s; releasing ownership"
                    resetOwnership("shower ON not confirmed on minute tick")
                }
            } else {
                resetOwnership("fan OFF reported on minute tick")
            }
        } else if (physical == "on") {
            acknowledgeShowerOn("minute watchdog")
            evaluateShower(t)
        }
    } else if (state.ffMode == "MANUAL") {
        if (physical == "off") {
            endManualEarly("minute watchdog detected OFF")
        } else if (physical == "on") {
            Long deadline = (state.ffStartedAtMs ?: t) as Long
            deadline += minutesMs(prefInt("manualMinutes", 20, 1, 180))
            if (t >= deadline) finishManual("minute watchdog")
        }
    } else if (state.ffMode == "IDLE" && physical == "on" && state.ffAwaitingOff != true) {
        // Lost switch event fallback: not an additional timer when already owned.
        startManual("minute watchdog discovered fan ON without ownership")
    }

    // B20 maintained a six-element rolling history once per minute.
    // v3 generalizes the number of samples to windowMinutes + 1.
    Double rh = readHumidity()
    if (rh != null) {
        state.ffLastRh = rh
        advanceHumidityHistory(rh)
    }
    checkHumidityEventAge(t)
    checkPendingOffReport(t)
}

def manualTimeoutHandler(Map data = null) {
    if (enableControl != true || state.ffMode != "MANUAL") return
    if (data?.generation == null || (data.generation as Integer) != (state.ffGeneration as Integer)) {
        log.debug "FOXWARE FoxyFan: Ignored stale manual callback generation=${data?.generation}"
        return
    }
    Long start = state.ffStartedAtMs as Long
    Long due = start + minutesMs(prefInt("manualMinutes", 20, 1, 180))
    if (now() < due) {
        armManualTimeout()  // scheduled jobs may fire a little early
        return
    }
    String physical = fanValue()
    if (physical == "on") finishManual("scheduled timer")
    else if (physical == "off") endManualEarly("scheduled callback observed fan already OFF")
    else {
        log.warn "FOXWARE FoxyFan: Manual timeout cannot read switch state; check device manually"
        resetOwnership("manual timeout with unknown switch state")
    }
}

// ─── Ownership, timers, and fan commands ────────────────────────────────────

private void startManual(String reason) {
    if (state.ffMode != "IDLE" || state.ffAwaitingOff == true || fanValue() != "on") return
    moveTo("MANUAL", reason)
    state.ffStartedAtMs = now()
    state.ffDrySinceMs = null
    state.ffShowerBase = null
    armManualTimeout()
    log.info "FOXWARE FoxyFan: Manual ventilation started; ${prefInt('manualMinutes',20,1,180)}-minute timer armed (${reason})"
    sendNotice("notifyManualStart", "Manual FoxFlatus ventilation started; ${prefInt('manualMinutes',20,1,180)}-minute timer armed (${reason}).")
}

private void endManualEarly(String reason) {
    if (state.ffMode != "MANUAL") return
    Long elapsedMs = state.ffStartedAtMs == null ? 0L : Math.max(0L, now() - (state.ffStartedAtMs as Long))
    Integer elapsedMin = (elapsedMs / 60000L) as Integer
    resetOwnership("manual ventilation ended early: ${reason}")
    log.info "FOXWARE FoxyFan: Manual ventilation ended early (${reason})"
    sendNotice("notifyManualEarlyOff", "Manual FoxFlatus ventilation ended early after about ${elapsedMin} minute(s) (${reason}).")
}

private void armManualTimeout() {
    if (state.ffMode != "MANUAL" || state.ffStartedAtMs == null) return
    Long due = (state.ffStartedAtMs as Long) + minutesMs(prefInt("manualMinutes", 20, 1, 180))
    Long remaining = Math.max(1L, due - now())
    Long seconds = Math.max(1L, ((remaining + 999L) / 1000L) as Long)
    runIn(seconds, "manualTimeoutHandler", [overwrite: true, data: [generation: state.ffGeneration]])
}

private void startShower(Double rh, Double baseline, Double rise) {
    if (state.ffMode != "IDLE" || state.ffAwaitingOff == true || fanValue() != "off") return
    unschedule("manualTimeoutHandler")
    moveTo("SHOWER", "humidity rise ${rise} RH points")
    // IMPORTANT: commit ownership before invoking fan.on(), so the resulting
    // ON event cannot accidentally arm FoxFlatus (old B20 race fix).
    state.ffStartedAtMs = now()
    state.ffShowerBase = baseline
    state.ffDrySinceMs = null
    state.ffAwaitingOn = true
    state.ffOnRequestedAtMs = now()
    log.info "FOXWARE FoxyFan: Shower rise detected: RH=${rh}%, baseline=${baseline}%, rise=${rise} points"
    try {
        fanSwitch.on()
        scheduleShowerOnReadback()
        sendNotice("notifyShower", "Shower detected. RH=${rh}%, baseline=${baseline}%, rise=${rise} points. Fan ON requested.")
    } catch (Exception ex) {
        log.error "FOXWARE FoxyFan: Fan ON command failed: ${ex.message}"
        resetOwnership("fan ON command failure")
    }
}

/** No unconditional sensor refresh: humidity change events already supply RH. */
private void scheduleShowerOnReadback() {
    if (state.ffMode != "SHOWER" || state.ffAwaitingOn != true) return
    Integer generation = state.ffGeneration as Integer
    Long age = state.ffOnRequestedAtMs == null ? 0L :
        Math.max(0L, now() - (state.ffOnRequestedAtMs as Long))
    // Deadlines remain anchored to the original ON request after app updates.
    if (postOnRefresh != false) {
        Long refreshWait = Math.max(1L, ((Math.max(0L, 3000L - age) + 999L) / 1000L) as Long)
        runIn(refreshWait, "refreshFanAfterOn", [overwrite: true, data: [generation: generation]])
    }
    Long verifyWait = Math.max(1L, ((Math.max(0L, 20000L - age) + 999L) / 1000L) as Long)
    runIn(verifyWait, "verifyFanAfterOn", [overwrite: true, data: [generation: generation]])
}

private void acknowledgeShowerOn(String reason) {
    if (state.ffMode != "SHOWER" || state.ffAwaitingOn != true) return
    state.ffAwaitingOn = false
    state.ffOnRequestedAtMs = null
    if (debugLogging == true) log.debug "FOXWARE FoxyFan: Shower fan ON confirmed (${reason})"
}

def refreshFanAfterOn(Map data = null) {
    if (enableControl != true || !fanSwitch || postOnRefresh == false) return
    if (data?.generation == null || (data.generation as Integer) != (state.ffGeneration as Integer)) return
    if (state.ffMode != "SHOWER" || state.ffAwaitingOn != true) return
    try {
        if (fanSwitch.hasCommand("refresh")) {
            log.info "FOXWARE FoxyFan: Requesting fan Refresh after shower ON; current reported switch=${fanValue()}"
            fanSwitch.refresh()
        } else {
            log.warn "FOXWARE FoxyFan: Selected fan driver has no Refresh command; awaiting native ON report"
        }
    } catch (Exception ex) {
        log.warn "FOXWARE FoxyFan: Post-ON Refresh failed: ${ex.message}"
    }
}

def verifyFanAfterOn(Map data = null) {
    if (enableControl != true || !fanSwitch) return
    if (data?.generation == null || (data.generation as Integer) != (state.ffGeneration as Integer)) return
    if (state.ffMode != "SHOWER" || state.ffAwaitingOn != true) return
    if (fanValue() == "on") {
        acknowledgeShowerOn("20-second readback")
    } else {
        log.warn "FOXWARE FoxyFan: Shower fan ON not confirmed after 20 seconds; reported switch=${fanValue()}. " +
            "Releasing ownership; inspect fan/device manually."
        resetOwnership("shower ON not confirmed")
    }
}

private void evaluateShower(Long t) {
    if (state.ffMode != "SHOWER" || state.ffStartedAtMs == null || state.ffShowerBase == null) return
    Long elapsed = t - (state.ffStartedAtMs as Long)
    Long maxMs = minutesMs(prefInt("maximumShowerMinutes", 80, 2, 240))

    // v3 improvement: hard maximum takes precedence and only ONE ending runs.
    // B20 evaluated maximum as an independent IF, allowing double actions.
    if (elapsed >= maxMs) {
        endRun("MAXIMUM", "Maximum ${prefInt('maximumShowerMinutes',80,2,240)}-minute shower runtime reached")
        return
    }

    Long minMs = minutesMs(prefInt("minimumShowerMinutes", 15, 1, 120))
    if (elapsed < minMs) return

    Double rh = readHumidity()
    if (rh == null) {
        state.ffDrySinceMs = null
        return
    }
    Double threshold = (state.ffShowerBase as Double) + prefInt("dryAllowance", 9, 0, 25)
    if (rh > threshold) {
        state.ffDrySinceMs = null
        return
    }
    if (state.ffDrySinceMs == null) {
        state.ffDrySinceMs = t
        if (debugLogging == true) log.debug "FOXWARE FoxyFan: Dry confirmation started at RH=${rh}% <= ${threshold}%"
        return
    }
    if (t - (state.ffDrySinceMs as Long) >= minutesMs(prefInt("dryConfirmMinutes", 5, 1, 60))) {
        endRun("DRY", "Drying complete. RH=${rh}%, baseline=${state.ffShowerBase}%, threshold=${threshold}%")
    }
}

private void finishManual(String reason) {
    if (state.ffMode != "MANUAL" || fanValue() != "on") return
    endRun("MANUAL", "Manual ${prefInt('manualMinutes',20,1,180)}-minute ventilation complete (${reason})")
}

private void endRun(String type, String detail) {
    if ((type == "MANUAL" && state.ffMode != "MANUAL") ||
        (type != "MANUAL" && state.ffMode != "SHOWER")) return
    log.info "FOXWARE FoxyFan: ${detail}; requesting fan OFF"
    // Transition out of active ownership BEFORE sending switch OFF.
    // Prevent late/duplicate timer callbacks from affecting subsequent runs.
    resetOwnership("${type}: ${detail}")
    state.ffAwaitingOff = true
    state.ffOffRequestedAtMs = now()
    state.ffOffWarningIssued = false
    try {
        fanSwitch.off()
        // Give the relay time to finish moving before asking the driver to
        // read back its actual state. Guarded callback cannot affect a new run.
        if (postOffRefresh != false) {
            runIn(3, "refreshFanAfterOff", [overwrite: true,
                data: [generation: state.ffGeneration]])
        }
    } catch (Exception ex) {
        log.error "FOXWARE FoxyFan: Fan OFF command failed; check switch manually: ${ex.message}"
    }
    if (type == "DRY") sendNotice("notifyDry", detail + ". Fan OFF requested.")
    else if (type == "MAXIMUM") sendNotice("notifyMaximum", detail + ". Fan OFF requested.")
    else if (type == "MANUAL") sendNotice("notifyManual", detail + ". Fan OFF requested.")
}

/**
 * Refresh is OPTIONAL: capability.switch promises on/off but not refresh().
 * Only refresh if the same OFF operation is still awaiting a report.
 * The generation token protects a new MANUAL or SHOWER run from stale jobs.
 */
def refreshFanAfterOff(Map data = null) {
    if (enableControl != true || !fanSwitch || postOffRefresh == false) return
    if (data?.generation == null ||
        (data.generation as Integer) != (state.ffGeneration as Integer)) return
    if (state.ffMode != "IDLE" || state.ffAwaitingOff != true) return
    try {
        if (fanSwitch.hasCommand("refresh")) {
            log.info "FOXWARE FoxyFan: Requesting fan Refresh after OFF; current reported switch=${fanValue()}"
            fanSwitch.refresh()
        } else {
            log.warn "FOXWARE FoxyFan: Selected fan driver has no Refresh command; " +
                "waiting for native switch report"
        }
    } catch (Exception ex) {
        log.warn "FOXWARE FoxyFan: Post-OFF Refresh failed: ${ex.message}"
    }
}

private void moveTo(String target, String detail) {
    state.ffGeneration = ((state.ffGeneration ?: 0) as Integer) + 1
    state.ffMode = target
    state.ffLastTransition = "${new Date().toString()}: ${target} (${detail})"
}

private void resetOwnership(String detail) {
    unschedule("manualTimeoutHandler")
    unschedule("refreshFanAfterOn")
    unschedule("verifyFanAfterOn")
    moveTo("IDLE", detail)
    state.ffAwaitingOn = false
    state.ffOnRequestedAtMs = null
    state.ffStartedAtMs = null
    state.ffDrySinceMs = null
    state.ffShowerBase = null
}

// ─── RH history, settings, diagnostics, notification helpers ────────────────

private void seedHumidityHistory(Double rh) {
    Integer count = prefInt("historyMinutes", 5, 1, 30) + 1
    List entries = []
    count.times { entries << rh }
    state.ffRhHistory = entries
    if (debugLogging == true) log.debug "FOXWARE FoxyFan: Seeded ${count} RH samples at ${rh}%"
}

private void advanceHumidityHistory(Double rh) {
    Integer count = prefInt("historyMinutes", 5, 1, 30) + 1
    List entries = state.ffRhHistory instanceof List ? new ArrayList(state.ffRhHistory) : []
    if (!entries) {
        seedHumidityHistory(rh)
        return
    }
    entries << rh
    while (entries.size() > count) entries.remove(0)
    state.ffRhHistory = entries
}

private Double readHumidity() {
    if (!humiditySensor) return null
    return numericRh(humiditySensor.currentValue("humidity"))
}

private Double numericRh(Object value) {
    if (value == null) return null
    try {
        Double rh = value.toString().toDouble()
        if (rh.isNaN() || rh.isInfinite() || rh < 0d || rh > 100d) return null
        return rh
    } catch (Exception ignored) {
        return null
    }
}

private String fanValue() {
    if (!fanSwitch) return "unknown"
    return fanSwitch.currentValue("switch")?.toString() ?: "unknown"
}

private boolean modeAllowsShower() {
    if (!allowedModes) return true
    List chosen = allowedModes instanceof Collection ? allowedModes as List : [allowedModes]
    return chosen.collect { it.toString() }.contains(location.mode?.toString())
}

private Integer prefInt(String name, Integer defaultValue, Integer low, Integer high) {
    def raw = settings[name]
    Integer v = defaultValue
    try { if (raw != null) v = raw.toString().toInteger() }
    catch (Exception ignored) { v = defaultValue }
    return Math.min(high, Math.max(low, v)) as Integer
}

private Long minutesMs(Integer minutes) {
    return (minutes as Long) * 60000L
}

private void checkHumidityEventAge(Long t) {
    Long since = state.ffLastHumidityEventMs != null ? (state.ffLastHumidityEventMs as Long) :
        (state.ffEnabledAtMs != null ? (state.ffEnabledAtMs as Long) : t)
    Long limit = minutesMs(prefInt("staleNoticeMinutes", 30, 5, 240))
    if (t - since >= limit && state.ffStaleWarned != true) {
        state.ffStaleWarned = true
        log.warn "FOXWARE FoxyFan: No humidity CHANGE events for ${prefInt('staleNoticeMinutes',30,5,240)}+ minutes. " +
            "This may indicate stable RH or a sensor reporting problem."
    }
}

private void checkPendingOffReport(Long t) {
    if (state.ffAwaitingOff != true || state.ffOffWarningIssued == true) return
    Long requestedAt = (state.ffOffRequestedAtMs ?: t) as Long
    if (t - requestedAt >= 120000L) {
        state.ffOffWarningIssued = true
        log.warn "FOXWARE FoxyFan: Fan OFF report not seen 2+ minutes after OFF command. " +
            "Inspect fan device; new manual ownership is deliberately suppressed."
    }
}

/**
 * One distinct prefix per installation; no duplicated FOXWARE FoxyFan name.
 * Installed-app custom label wins (e.g. "Guest Bathroom"). When the default
 * label has not been customized, use that installation's configured fan name
 * (e.g. "Master Bath Fan"). App ID is a last-resort unambiguous identifier.
 */
private String notificationIdentity() {
    final String brand = "FOXWARE FoxyFan"
    String installedLabel = app?.label?.toString()?.replaceAll(/\s+/, " ")?.trim()
    if (installedLabel && !installedLabel.equalsIgnoreCase(brand)) {
        // Already branded labels are complete identifiers; don't prefix twice.
        if (installedLabel.toLowerCase().startsWith(brand.toLowerCase() + " ")) {
            return installedLabel
        }
        return "${brand} (${installedLabel})"
    }
    String fanName = fanSwitch?.displayName?.toString()?.replaceAll(/\s+/, " ")?.trim()
    if (fanName) return "${brand} (${fanName})"
    return "${brand} (instance ${app.id})"
}

private void sendNotice(String settingName, String text) {
    if (settings[settingName] != true || !notificationDevices) return
    String prefix = notificationIdentity()
    notificationDevices.each { destination ->
        try {
            destination.deviceNotification("${prefix}: ${text}")
        } catch (Exception ex) {
            log.warn "FOXWARE FoxyFan: Notification to ${destination?.displayName} failed: ${ex.message}"
        }
    }
}
