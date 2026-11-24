# Changelog — FOXWARE FoxyFan

## [3.0.4] — 2026-11-23 — Per-instance naming and notification identity

- Fix duplicate `FOXWARE FoxyFan (FOXWARE FoxyFan)` notification prefix across all six alert types.
- Centralize identity in `notificationIdentity()`: use the installed app's custom label, otherwise the selected fan's `displayName`, otherwise the app's unique ID.
- Do not repeat the `FOXWARE FoxyFan` brand when the label already begins with it.
- Add native Hubitat **Instance naming** preference (`label`) directly to the configuration screen; setting it changes `app.label`, so each bathroom is named in the Apps list and in notification prefixes.
- Show the computed **Notification identity** in the existing app status screen to simplify multi-bathroom setup and verification.
- No changes to device control, humidity logic, timer/refresh implementation, notification toggles, or existing stored preference keys. Only a native label UI element is added. Existing instances do not need reinstalling.
- Static checks complete; v3.0.4 label UI and notification display not yet tested on a live hub.

## [3.0.3] — 2026-11-23 — Activation-label cleanup

- Rename activation toggle to **Enable Fan Control (can be disabled for testing)**, removing obsolete cutover language.
- Bump visible UI/app installation version to v3.0.3; source header records the completed v3.0.2 native shower/full-drying test.
- No changes to control logic, saved preference keys, timers, configured thresholds or device handling.
- Retains new-install default OFF; saved activation setting on existing installations is preserved.

## [3.0.2] — 2026-11-23 — Shower ON readback and FoxFlatus notifications

- Live screenshot confirmation: v3.0.1 2-minute manual timer began at 6:07:54.333 PM, requested OFF at 6:09:54.323 PM, and device logged OFF at 6:09:54.436 PM; the user confirmed correct status reporting. This validates the native manual path, **not** shower automation.
- Audit proves `endRun()` already uses post-OFF refresh for manual, dry completion and automatic maximum; no duplicated OFF logic.
- Add optional delayed shower-start post-ON refresh (`postOnRefresh`, default ON); only needed while ON not acknowledged. Guard stale OFF reports during the 20-second ON confirmation window with generation-protected scheduling.
- Add `notifyManualStart` and `notifyManualEarlyOff` switches (both OFF by default); preserve existing `notifyManual` timer-completion preference and clarify its label.
- Remove incorrect “no live validation” sentence from the native app configuration screen; update release/test documentation with the observed results and untested v3.0.2 paths.
- Leave production thresholds and state/timer behavior unchanged except explicitly documented ON readback safeguards.


## [3.0.1] — 2026-11-23 — Post-OFF refresh patch (native testing)

- First live v3.0.0 two-minute FoxFlatus cycle **physically turned the fan OFF**; Hubitat continued to show the device ON (stale reported state).
- Add optional post-OFF 3-second delayed `refresh()` for drivers that support the command (`postOffRefresh` defaults ON).
- Retain pending-OFF interlock and generation token guard; do not fake an OFF report or start a new manual timer from stale ON state.
- No algorithm/timer/setpoint changes. Production behavior still requires hub validation.


## [3.0.0] — 2026-11-23 — Native Hubitat draft, NOT production validated

- First reusable native Hubitat Groovy app, based on the exact running webCoRE v2.0.3 Build 20 export.
- Multiple independent installed app instances for Master, Guest, and additional bathrooms.
- Configurable humidity sensor, fan switch, allowed modes, optional notification devices.
- Configurable shower rise, RH window, minimum, dry allowance/confirmation, maximum, and manual runtime.
- Default parameter values preserve B20: +6, 5-minute baseline, 15-minute minimum, +9, 5-minute dry confirmation, 80-minute max, 20-minute manual.
- Replaces cross-piston global-state coordination with `IDLE/MANUAL/SHOWER` local state machine; `singleThreaded` top-level callbacks.
- Adds generation-token protection against stale scheduled manual timers and pending-OFF acknowledgment guard.
- Adds lightweight diagnostics and sensor event-age notice.
- Improves double-OFF edge case by making the hard maximum single-outcome priority.
- Native controller disabled by default to avoid accidental double ownership during migration.
- Created release snapshot, SHA-256 manifest, reference export, design documentation, and test plan.

## [2.0.3 Build 20] — 2026-11-23 — webCoRE reference (existing, not modified)

- Successfully merged FoxFlatus manual ventilation and automatic shower ventilation into one piston.
- Manual timer tested in a real full cycle; shower-mode testing of this exact build still planned at time of reference capture.
- Source preserved in `reference/FOXWARE-FoxyFan-v2.0.3-B20.webcore`.
