# FOXWARE FoxyFan v3.0.4

**Native Hubitat Elevation Groovy App — Native manual and shower cycles live-tested**
**Reference:** The user-supplied, working webCoRE **FoxyFan v2.0.3 Build 20** (November 23, 2026).
**Status:** v3.0.2 has completed live manual FoxFlatus and automatic shower/drying cycles on Hubitat. v3.0.4 updates the notification identity formatter and adds an installed-app naming control; this revision is awaiting Hubitat validation.

FoxyFan is a reusable bathroom exhaust fan controller. One installed app instance owns **one** humidity sensor and **one** fan switch. Install another independent instance to manage a guest bathroom. No webCoRE runtime, external server, device driver, or parent app is required.

## Project layout

```text
FOXWARE-FoxyFan/
├── src/FOXWARE-FoxyFan.groovy
├── docs/README.md, DESIGN.md, CHANGELOG.md, and version diffs
├── tests/TEST-PLAN.md, static_check.py, test_notification_identity.py
├── reference/FOXWARE-FoxyFan-v2.0.3-B20.webcore
└── Releases/
    ├── v3.0.0/
    ├── v3.0.1/
    ├── v3.0.2/
    ├── v3.0.3/
    └── v3.0.4/
        ├── FOXWARE-FoxyFan-v3.0.4.groovy
        ├── RELEASE-NOTES.md
        └── SHA256SUMS
```

## What it does

- Detects a shower by a configurable **+6 RH-point** rise over a **five-minute** rolling reference (six samples including the current minute), while fan is OFF and location mode is eligible.
- Claims automatic ownership **before** sending fan ON, preserving the important Build 20 ordering.
- Keeps automatic ventilation for at least **15 minutes**; once RH falls to **baseline +9 RH points**, confirms the condition for **5 minutes** and then requests fan OFF.
- Stops automatic ventilation at the hard **80-minute** maximum regardless of humidity.
- Recognizes a physical/manual fan ON while not shower-owned and runs the fan for **20 minutes**, with early-OFF cancellation.
- Recovers from missed ON events using a once-per-minute watchdog; detects missing OFF acknowledgments without re-arming an old timer.
- Sends optional push notifications to selected Hubitat `Notification` devices, now with **separate manual start, manual timeout, and early-off toggles**.
- **v3.0.4 instance naming:** Enter **Name this FoxyFan instance** in the native app preferences. This updates Hubitat’s actual installed-app name (`app.label`) and the notification identity; use `Master Bathroom` and `Guest Bathroom`. No more `FOXWARE FoxyFan (FOXWARE FoxyFan)`.
- On shower startup, optionally requests fan `refresh()` three seconds after the ON command **only if the ON report has not arrived**. A guarded 20-second readback prevents an immediately stale OFF report from prematurely canceling shower mode.
- Exposes settings, status, and recent state transition text on a native Hubitat app page.

### Build 20 defaults (all configurable)

| Setting | Native app default | Meaning |
|---|---:|---|
| Shower trigger | **+6 RH points** | Current RH minus oldest RH sample |
| Baseline history | **5 minutes** | Six minute-spaced samples (initially seeded) |
| Minimum automatic runtime | **15 minutes** | No drying stop before this |
| Drying allowance | **+9 RH points** | Stop eligibility at RH ≤ original baseline +9 |
| Dry confirmation | **5 minutes** | Consecutive elapsed time after first eligible minute |
| Maximum automatic runtime | **80 minutes** | Safety timeout |
| Manual runtime | **20 minutes** | FoxFlatus timer |
| Humidity event-age warning | **30 minutes** | Informational stale/unchanged-RH warning |
| Allowed modes | **Unrestricted until selected** | Select your existing Home / Home-NoMotion / Guest modes to match B20 |
| Push notifications | **On for shower / dry / max; off for three manual events** | Start, timer completion, early manual OFF are independent toggles |
| **Enable native control** | **OFF** | Deliberate safe-by-default cutover |

**Two easy-to-confuse values:** +6 is the *start* trigger. +9 is the *drying stop* allowance. They are independent and both editable.

## Safe installation and migration from webCoRE

1. **Leave working Build 20 enabled until ready to cut over.** Preserve its export and a copy of current settings.
2. Open Hubitat's **Apps Code** page; choose **New App** and paste `src/FOXWARE-FoxyFan.groovy` (or the identical release file). Save the code. Correct any Groovy/platform errors before proceeding. Do not mistake a successful save for functional validation.
3. In Hubitat **Apps**, choose **Add User App** → **FOXWARE FoxyFan**. Set **Instance naming → Name this FoxyFan instance** (e.g., `Master Bathroom`). Select the appropriate humidity sensor and fan switch, optionally Notification devices.
4. Select the permitted modes to **match Build 20**: Home, Home-NoMotion, Home - Guest Mode (provided those modes exist on your hub). If left blank, **automatic shower detection is allowed in all modes**.
5. Verify settings. **Leave `Enable Fan Control (can be disabled for testing)` OFF** while inspecting the new app. The preview is inert while disabled.
6. Schedule a quiet cutover with the fan OFF and no shower in progress. **Pause the existing FoxyFan webCoRE piston** (and make sure the old 20-minute piston is already paused). Confirm no other automation is controlling this fan.
7. Set `Enable Fan Control (can be disabled for testing)` to ON in the native app and save. Watch Logs; verify the app reports `Ready`, fan OFF, mode `IDLE`.
8. Perform the tests in `tests/TEST-PLAN.md`, starting with manual ON/OFF and a shortened manual timeout, then a full timed test and shower simulation/real shower.
9. If problems occur, switch the native app **OFF**, confirm timers cleared, then re-enable the known-good webCoRE piston. **Never leave both controllers enabled** for the same fan.
10. After a complete test cycle, restore your production settings (20-minute manual / 15-minute minimum / +6 / +9 / 5-minute dry / 80-minute maximum, or your chosen changes).

**Multi-bathroom setup:** Install another `FOXWARE FoxyFan` instance from **Add User App**, selecting guest bathroom devices and independent settings. Each instance has separate subscriptions, timers, `state`, and installation identity. **Never assign the same physical fan switch to two enabled FoxyFan instances.**

## Naming multiple bathroom instances in notifications

Each instance shares the same source code but has independent settings, local `state`, schedules, and notification identity. Hubitat provides an **installed-app label** (`app.label`) for each instance. Give them descriptive labels such as **Master Bathroom** and **Guest Bathroom** using **Instance naming → Name this FoxyFan instance** at the top of the FoxyFan settings page. This is Hubitat’s built-in `label` control, not a separate text preference. It changes the name listed under **Apps**, as well as the prefix used in notifications.

Examples with v3.0.4:

| Installed-app label | Selected fan switch | Notification prefix |
|---|---|---|
| `FOXWARE FoxyFan` (default) | `Master Bath Fan` | `FOXWARE FoxyFan (Master Bath Fan)` |
| `FOXWARE FoxyFan` (default) | `Guest Bath Fan` | `FOXWARE FoxyFan (Guest Bath Fan)` |
| `Master Bathroom` | `Master Bath Fan` | `FOXWARE FoxyFan (Master Bathroom)` |
| `FOXWARE FoxyFan - Guest Bath` | `Guest Bath Fan` | `FOXWARE FoxyFan - Guest Bath` |

A custom label takes precedence. If no custom label or fan name is available yet, the instance's Hubitat app ID is used as a last resort. To verify what will be sent, open the instance's **Activation and instance status** section and check **Notification identity**. Nothing in `deviceNotification()` adds the app identity automatically; FoxyFan explicitly formats this prefix once. All six notification event types use the same helper.

## Required device behavior

- Humidity device: implements Hubitat `RelativeHumidityMeasurement` (numeric `humidity` attribute). Prefer prompt **1% RH-change reports** in real bathroom conditions. If the driver only reports every 5 RH points or once an hour, a +6 shower rise may be delayed or missed.
- Fan device: implements Hubitat `Switch` with `on()`, `off()`, and switch change reports. Version 3.0.1 **optionally** requests `Refresh` after app-issued OFF; v3.0.2 does the same for shower ON if no ON report has arrived. If unavailable, a warning is logged. An ON request that remains unconfirmed after 20 seconds relinquishes shower ownership and requires inspection; it does not send an invented state or another ON/OFF command.
- Notification device: implements Hubitat `Notification`, with `deviceNotification(text)`.
- A stable RH reading may produce no change events for a long time. The event-age warning **does not distinguish steady RH from a dead sensor**. Automatic maximum runtime does not depend on humidity reporting.

## Status and limitations

This release is under **active platform validation**. Native manual operation has been tested on the hub: on November 23, 2026, the 2-minute test began at 6:07:54 PM and the timer requested OFF at 6:09:54 PM; the fan driver logged OFF. v3.0.2 shower ON confirmation, manual-start/completion push notifications, and full drying completion have been successfully observed on the hub. v3.0.4 instance naming UI and notification identity formatting are pending validation. Hubitat's scheduler is not a real-time clock: a deadline is **eligible to run at or after** the configured duration, but execution may be late. Command success is not proof of hardware state; device feedback governs reconciliation.

Behavioral differences from Build 20 are explicitly listed in `docs/DESIGN.md` and the versioned release notes. The old cross-piston `@humidtrip` global is intentionally **not** used. There is no automatic transfer from MANUAL mode to SHOWER mode while the fan is ON, matching Build 20. No network calls, credentials, or personal data are necessary.

## Source of Hubitat API design

- [App definition and instance settings](https://docs2.hubitat.com/en/developer/app/definition)
- [App preferences and capability selectors](https://docs2.hubitat.com/en/developer/app/preferences)
- [Hubitat common methods (`subscribe`, `runIn`, `runEvery1Minute`)](https://docs2.hubitat.com/en/developer/common-methods-object)
- [Driver capabilities](https://docs2.hubitat.com/en/developer/driver/capability-list)
