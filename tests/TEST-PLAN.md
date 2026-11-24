# FoxyFan v3.0.2 — Test plan / release acceptance

**Status:** v3.0.1 installed on Hubitat and completed a successful live two-minute manual ON → timed OFF test on November 23, 2026; screenshot shows driver OFF report. The new v3.0.2 shower-ON readback and manual notifications remain untested. Do **not** enable native and webCoRE controllers simultaneously on the same fan.

## Stage 0 — Static / archive

- [ ] Release Groovy exactly matches `src/FOXWARE-FoxyFan.groovy` (SHA-256 or `cmp`).
- [ ] `Releases/v3.0.2/SHA256SUMS` validates the release files.
- [ ] Reference B20 file matches archived upload; check detection +6, dry +9, 15, 5, 80, 20, and three PUSH messages.
- [x] `python3 tests/static_check.py` passes. **This is only a static sanity check, not an executable Groovy test.**

## Stage 1 — Hubitat app compilation and inert installation

- [x] Earlier v3.0.0/3.0.1 compiled in **Apps Code** and ran on the hub; v3.0.2 still requires saving/testing.
- [ ] Add first `FOXWARE FoxyFan` user app; verify device selectors and parameter input ranges render.
- [ ] `enableControl` initially **OFF**. Turning fan ON/OFF with native app disabled must cause **zero native commands**.
- [ ] Pick humidity and fan devices; optionally notification devices. Set allowed modes to same three as B20.
- [ ] Logs show no errors; no subscriptions or scheduled control jobs while disabled.

## Stage 2 — Manual FoxFlatus tests (webCoRE paused)

- [ ] With fan initially OFF, enable native app; verify `IDLE`.
- [ ] Set manual timer temporarily **2 minutes**, turn fan ON physically. Verify `MANUAL` and timestamp.
- [ ] Send duplicate ON report or refresh device; the timer **must not restart**.
- [ ] Before timeout, physically turn fan OFF: immediate or watchdog `IDLE`, no late OFF action.
- [ ] Turn fan ON again. Leave it ON: OFF requested **at or after** 2 minutes, exactly one completion message.
- [ ] Confirm app stays IDLE after its own OFF event, and does not re-arm manual timer from stale ON.
- [ ] Restore manual timer **20 minutes** (or production choice); repeat a full normal cycle.
- [ ] Test app update while MANUAL: original start time retained; remaining timer rescheduled.

## Stage 3 — Shower cycle (real shower or controlled RH events)

- [ ] Restore +6 detection / five-minute RH history / +9 dry allowance / 15-minute minimum / 5-minute confirmation / 80-minute max.
- [ ] Confirm no auto start before initial RH baseline is seeded; note initial-seed behavior.
- [ ] In allowed mode, fan OFF, raise reported RH by >=6 points above historical baseline. Verify `SHOWER` and ON.
- [ ] Confirm emitted fan ON event does not create a MANUAL timer.
- [ ] Confirm RH below threshold early does **not** stop fan before the minimum runtime.
- [ ] After minimum runtime, hold RH <= baseline+9 for 5 minutes. Verify single OFF and dry notification.
- [ ] Raise RH above baseline+9 partway through dry confirmation; timer resets. Later dry period starts a fresh 5 minutes.
- [ ] Run with RH above dry threshold; verify hard 80-minute timeout. For accelerated testing use shorter configured maximum with a compatible minimum; restore defaults afterwards.
- [ ] Set drying and hard maximum deadlines to coincide; verify only one completion notification (MAXIMUM wins).
- [ ] Physically turn fan OFF during SHOWER; app returns to IDLE and does not command a late OFF.
- [ ] In disallowed mode, shower rise cannot auto-trigger; manual timer still works.
- [ ] While fan was manually started, simulate rapid humidity rise; confirm it remains MANUAL (Build 20 parity).

## Stage 4 — Faults, races, and recovery

- [ ] Late ON event after app issued OFF does not re-arm FoxFlatus before an OFF acknowledgment.
- [ ] Missed OFF event is recovered if minute watchdog sees current switch value OFF.
- [ ] Suppress OFF acknowledgment/currentValue update: warning after ~2 minutes; no timer rearm; test recovery after OFF update.
- [ ] Sensor unchanged beyond configured stale-notice interval logs warning once; humidity event clears warning.
- [ ] Invalid humidity (non-number/outside 0..100) does not trigger a shower or crash app.
- [ ] Hub restart/reinitialize during MANUAL: existing timestamp maintained, timer rearmed.
- [ ] Hub restart/reinitialize during SHOWER: previous baseline and start persisted, fan state reconciled.
- [ ] Change selected fan while running: old ownership discarded without issuing OFF to old or new fan.
- [ ] Disable app during manual/shower run: unschedules work **without changing the physical fan**.
- [ ] Turn back ON only after confirming physical fan state and appropriate cutover.
- [ ] Notification device unavailable: warning logged, fan control continues.

## Stage 5 — Independent guest bathroom installation

- [ ] Install second FoxyFan instance (same Apps Code source).
- [ ] Configure **different physical** fan and humidity sensor plus independent thresholds (e.g. manual 30, auto maximum 60).
- [ ] Operate Guest Bathroom without changing Master Bathroom state/timers.
- [ ] Operate Master Bathroom without changing Guest Bathroom state/timers.
- [ ] Verify selected push notifications identify their respective app installation label.
- [ ] Verify no two enabled instances select the same physical fan.

## Pass criteria and rollback

Release only after all applicable checks pass on real Hubitat hardware with logs and time stamps preserved. Any unexpected OFF/ON, overlapping controller, duplicate trigger, unscheduled fan shutdown, Groovy exception, or missing subscription is a release blocker. For rollback: toggle native control OFF, verify old callbacks are unscheduled, confirm fan safe, then re-enable the previously working Build 20 piston. Never overlap.

## Recording template

| Test | Date/time | Hub platform | Fan device/driver | Humidity device/reporting | Result | Logs/notes |
|---|---|---|---|---|---|---|
| Manual 2-min | 2026-11-23 6:07:54–6:09:54 PM | Hubitat live | Master Bath Fan | Master Bath Motion Sensor | PASS (v3.0.1) | ON event, timer OFF request, device OFF log screenshot |
| Manual full | | | | | NOT RUN | |
| Shower dry | | | | | NOT RUN | |
| Auto max | | | | | NOT RUN | |
| Double ownership | | | | | NOT RUN | |
| Guest independent | | | | | NOT RUN | |


## Test v3.0.1 — Refresh after app-issued OFF

- [ ] With postOffRefresh enabled, start manual 2-minute run and confirm exactly one OFF request.
- [ ] About 3 seconds later, observe `Requesting fan Refresh after OFF` in live logs when the driver exposes Refresh.
- [ ] Confirm Hubitat switch attribute changes to OFF, not merely that the physical relay stopped.
- [ ] Verify no spurious new MANUAL run after OFF; `ffAwaitingOff` is cleared by actual reported state.
- [ ] Test Refresh setting OFF and verify no refresh is requested.
- [ ] Test driver without Refresh support and verify warning but no Groovy exception (on a suitable test device).
- [ ] Check duplicate/stale callback ignored after state generation changes.
- [ ] Confirm shower completion and max-timeout paths also call OFF then optional Refresh.
- [ ] Restore production 20-minute manual timeout after shortened test.


## Test v3.0.2 — Shower ON and FoxFlatus event notification choices

- [ ] Save new 3.0.2 Groovy in Apps Code; open installed app, confirm **v3.0.2** in header, all prior device assignments and settings persist. Click Done to reinitialize while IDLE.
- [ ] Confirm old erroneous development-draft/no-live-validation paragraph has disappeared.
- [ ] Confirm **Refresh fan state after shower-start ON** is enabled by default; verify post-OFF refresh remains enabled.
- [ ] Enable notifyManualStart, notifyManual (timeout) and notifyManualEarlyOff; select Notification devices.
- [ ] Start a 2-minute manual cycle: exactly one start push, exactly one timeout push, no early-off push; fan goes OFF and state reconciles.
- [ ] Start a manual cycle then turn OFF early: exactly one start push, one early-off push, **no** timeout push after scheduled deadline.
- [ ] With all three toggles OFF, repeat; expect logs/physical behavior but no manual pushes.
- [ ] Prove humidity rise launches SHOWER while fan OFF and allowed mode. Verify initial ON report is reflected, never starts MANUAL mode.
- [ ] If fan ON state is delayed, observe optional `Requesting fan Refresh after shower ON` at ~3s (only if driver supports it), and confirmation by ON report before 20s.
- [ ] If ON reports promptly, verify no unnecessary ON refresh is attempted.
- [ ] Simulate missing ON acknowledgment in a safe test environment: stale OFF events do not cancel shower immediately; after 20s controller warns and releases ownership. Inspect fan physically; do not assume it is off.
- [ ] With postOnRefresh OFF, verify no ON refresh occurs, but guarded 20-second confirmation still applies.
- [ ] Verify existing post-OFF refresh after drying and maximum timeout; no duplicate OFF commands or manual timer rearming.
- [ ] Update the app during an active timer only under supervised test; verify `ffGeneration` prevents stale callbacks.
- [ ] Restore manual timeout to production value and desired notification settings before leaving the app unattended.

## v3.0.3 — Cosmetic activation label upgrade verification

- [ ] In Apps Code, save v3.0.3 without compilation errors.
- [ ] Existing instance shows **FOXWARE FoxyFan v3.0.3**.
- [ ] Activation toggle reads **Enable Fan Control (can be disabled for testing)**.
- [ ] Existing `enableControl` setting remains as configured (no reset to OFF).
- [ ] Other settings, history/state indicators, and subscriptions remain intact.
- [ ] Leave fan control logic unchanged; no new full-cycle validation is implied.


## v3.0.4 — Instance naming and notification identity regression tests

- [ ] Existing Master Bath instance with default installed-app label shows `Notification identity: FOXWARE FoxyFan (Master Bath Fan)` (or its currently selected fan label). Trigger a manual-start alert and verify exactly one `FOXWARE FoxyFan` name in push body.
- [ ] Complete a shortened manual timer: end notification carries the same prefix; device still confirms OFF.
- [ ] Open **Instance naming → Name this FoxyFan instance** in the native FoxyFan configuration and enter `Master Bathroom`. Click **Done**; verify the installed app is listed as `Master Bathroom` in Hubitat Apps. Reopen, verify the status line and next alert use `FOXWARE FoxyFan (Master Bathroom)` without the previous duplicated name.
- [ ] Create a separate Guest Bathroom instance (with distinct humidity sensor and fan); name it `Guest Bathroom` using the same **Instance naming** setting and verify a different Apps-list entry and notification prefix on its own mobile push. Do not reuse the same fan between two enabled instances.
- [ ] If already branded as `FOXWARE FoxyFan - Guest Bath`, verify the app does not add the brand twice.
- [ ] Check all six push categories (manual start/completion/early OFF, shower start/dry/maximum) call shared helper. Do not force 75–80 minute physical runtime solely for this regression; source inspection for unchanged routing is acceptable.
- [ ] Inspect UI-only change without running fan: custom label persists across reopen/code update; no new subscriptions, changed timers or device actions from the naming/notification patch.
- [ ] Roll back safely if needed: disable fan control before a code replacement while a run is active; restore v3.0.3 Groovy from Releases. Avoid running any competing controllers.
