# FOXWARE FoxyFan v3.0.1 — Post-OFF Refresh Hotfix

**2026-11-23 | Controlled live testing | Hubitat platform validation in progress**

## Why

The user confirmed the native v3.0.0 two-minute manual FoxFlatus timer completed and **physically turned the fan OFF**, but Hubitat continued to report `switch = on` afterward. The v3.0.0 `endRun()` called only `fanSwitch.off()`; it did not request a status refresh.

## Minimal change

- After a successful `fanSwitch.off()` command issued by FoxyFan, schedule a **single Refresh readback 3 seconds later**, if enabled and supported by the driver.
- Only call `fanSwitch.refresh()` if `fanSwitch.hasCommand("refresh")`.
- Optional `postOffRefresh` preference defaults **true**; leave it OFF when dealing with drivers that do not provide useful refresh support.
- Require the same control-generation number, current mode `IDLE` and pending OFF report before refresh: prevents delayed callback from interfering with a subsequent run.
- Log the refresh attempt or missing command; never synthesize a switch OFF event or issue extra OFF commands.
- Existing `ffAwaitingOff` protection, independent per-bathroom state, and 2-minute warning remain unchanged.

## Deliberately unchanged

- All shower and manual timings, humidity history, controls and ownership paths.
- All baseline default setpoints (`+6`, `5min`, `15min`, `+9`, `5min`, `80min`, `20min`).
- No forced refresh on manual physical OFF; the fix is for **app-issued fan OFF commands**.
- Does not override a physical fan or manufacture state. If the device continues reporting ON after a Refresh request, check its driver, pairing, associations, or radio communication.

## Test steps

1. Save new Groovy code in **Apps Code**, keeping the original v3.0.0 release available for rollback.
2. In the installed FoxyFan app, verify `Refresh fan state after ... OFF` enabled, then Done/Save.
3. With the old webCoRE piston paused and fan initially OFF, set a 2-minute manual test timeout.
4. Physically turn fan ON; verify native `MANUAL` timer arms once.
5. After timeout, verify log `... requesting fan OFF`, physical switch OFF, then within roughly 3 seconds `Requesting fan Refresh after OFF`.
6. Verify Hubitat switch state changes to OFF and pending-off warning disappears; the fan stays OFF and no new manual timer starts.
7. Restore 20-minute manual setting after tests. Check early OFF, automatic shower cycle later.

This source was statically checked and packaged, **not executed on the user's Hubitat device by the assistant**.
