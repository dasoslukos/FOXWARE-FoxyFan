# FOXWARE FoxyFan v3.0.2 — Release Notes

**Release:** 2026-11-23
**Status:** Native app manual ventilation tested through v3.0.1; **v3.0.2 ON readback and new manual notifications pending live tests**.
**Baseline:** FOXWARE FoxyFan webCoRE v2.0.3 Build 20, archived unchanged in `reference/`.

## Changes from v3.0.1

1. Shower-start **ON reporting** now has optional delayed driver refresh, default enabled (`postOnRefresh`). After requesting ON, the app waits three seconds and calls `fanSwitch.refresh()` only if driver supports Refresh and an ON report hasn't arrived. A generation-guarded 20-second verification tolerates a temporarily stale OFF state; when ON remains unconfirmed it releases ownership and warns, without sending another ON/OFF command.
2. **Manual FoxFlatus event notifications:** New `notifyManualStart` and `notifyManualEarlyOff` toggles, default OFF; the existing `notifyManual` completion toggle retains its setting and is relabeled clearly. All notifications use the app's selected Notification devices and are independent of fan control.
3. **Truthful status:** Removed the obsolete UI claim that no live Hubitat validation occurred. v3.0.1 manual test is documented as passing; v3.0.2 new features remain untested.
4. **Audit outcome:** The existing post-OFF refresh helper is already shared by manual timeout, shower drying, and automatic maximum runtime. No duplicate OFF command logic added. Humidity sensors are not blindly refreshed after every new event.

## Installation / upgrading

- **Use** `FOXWARE-FoxyFan-v3.0.2.groovy` in Hubitat **Apps Code**, replacing the existing FoxyFan code; click **Save**.
- Open the **installed** FoxyFan app, verify v3.0.2 settings and click **Done** while the fan is idle so `updated()` reinitializes subscriptions and schedules.
- Retain your existing humidity sensor/fan selections, mode choices, timers and +6/+9 thresholds. The two *new* manual push switches default OFF; you may enable them independently. Existing manual timeout notifications retain the same `notifyManual` preference key.
- Keep the old webCoRE piston paused while native control is enabled. Do not operate two controllers on the same fan.
- Conduct the new tests in `tests/TEST-PLAN.md`. If shower ON status is delayed, check for a Refresh readback, and inspect the physical fan if ON remains unconfirmed after 20 seconds.

## Verification scope

- Source/release byte parity, static structure, checksum verification, reference-file integrity and ZIP archive validated locally.
- **Not** compiled or executed in Hubitat for v3.0.2. Your existing live v3.0.1 manual-cycle results do not establish correctness of the new shower handling.
- Nothing in this package has been deployed to the hub.

## Source API references

- https://docs2.hubitat.com/en/developer/common-methods-object
- https://docs2.hubitat.com/en/developer/driver/capability-list
