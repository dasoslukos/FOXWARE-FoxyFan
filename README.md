# FOXWARE FoxyFan 🦊🚿💨

**Smart, configurable bathroom exhaust-fan automation for Hubitat Elevation.**

FoxyFan automatically detects showers from a rapid relative-humidity (RH) increase, ventilates until the bathroom has had time to dry, and provides timed manual ventilation through the proudly named **FoxyFlatus** mode. It is a native Hubitat Groovy app: no webCoRE piston, external server, or custom companion driver is required.

**Current version:** `v3.0.4`
**Status:** Live-tested on Hubitat with one master-bathroom sensor/fan combination; additional hardware combinations and edge cases are still being tested.
**License:** [MIT](LICENSE)

## Features

- **Automatic shower detection:** Recognizes a configurable RH-point rise relative to a rolling historical reading, then requests fan ON.
- **Adaptive drying:** Enforces a minimum runtime, then requires humidity to remain at or below the original shower baseline plus a configurable allowance for a continuous confirmation period.
- **Maximum runtime safeguard:** Requests fan OFF at a separately configurable maximum shower runtime, even when the room has not dried sufficiently.
- **FoxyFlatus manual ventilation:** A physical/manual fan-ON event arms a configurable timer; switching the fan OFF early cancels the run.
- **Independent bathroom installations:** Install the same app more than once, select different devices, name each instance (e.g. **Master Bathroom** and **Guest Bathroom**), and configure each independently.
- **Optional push notifications:** Separate controls for shower start, drying complete, automatic maximum runtime, manual start, manual timeout, and manual early-OFF.
- **State-aware device reporting:** Verifies shower ON reports, requests a delayed fan `refresh()` when supported and needed, and guards against stale switch reports rearming an unwanted manual timer.
- **Local status and diagnostics:** Current operating mode, humidity, recent transition, humidity-change event age, and optional detailed logs.
- **Safe installation:** Fan control defaults to **disabled** until deliberately enabled.

## Requirements

| Component | Hubitat capability | Important notes |
| --- | --- | --- |
| Humidity sensor | `RelativeHumidityMeasurement` | Must expose a numeric `humidity` attribute and report changes promptly. Reporting every **1 RH point** is ideal. |
| Fan controller | `Switch` | Must support `on()`/`off()` and report switch state, including physical/manual activations. Use a relay/switch **rated for the fan motor/load**. |
| Push destination (optional) | `Notification` | Must support `deviceNotification(text)`. |

**Reporting frequency matters.** A sensor that sends frequent change events during a shower can detect the rise promptly even if it remains quiet for long periods when humidity is stable. A 30- or 60-minute period with no *change event* does not, by itself, establish that a sensor has failed. FoxyFan's event-age warning is informational.

The optional switch `refresh()` feature depends on the selected device driver. It requests a fresh report; it is **not** a substitute for a working physical fan or reliable switch-state reporting.

## Installation

1. In Hubitat Elevation, open **Apps Code → New App**.
2. Paste the contents of [`src/FOXWARE-FoxyFan.groovy`](src/FOXWARE-FoxyFan.groovy) (or the versioned [`v3.0.4` release source](Releases/v3.0.4/FOXWARE-FoxyFan-v3.0.4.groovy)), then click **Save**.
3. Open **Apps → Add User App → FOXWARE FoxyFan**.
4. Set **Name this FoxyFan instance** (for example, `Master Bathroom`). Choose a **humidity sensor** and **bathroom fan switch**.
5. Review the thresholds, runtime settings, allowed location modes, and optional notification devices.
6. Keep **Enable Fan Control (can be disabled for testing)** **OFF** until you are ready to test and cut over.
7. If replacing webCoRE or another fan automation, **pause/disable that automation first**. Verify the fan is OFF and there is no active shower cycle before enabling FoxyFan. Never allow two controllers to manage the same fan concurrently.
8. Turn **Enable Fan Control** ON and click **Done**. Test manual ON, early OFF, timeout, and an automatic shower cycle while observing Hubitat logs and fan state.

**Upgrading:** Update the existing code under **Apps Code** and click **Save**. You do not need to install a new app instance. Open each existing FoxyFan instance afterward, verify its options, and click **Done** to reinitialize subscriptions/schedules. Prefer upgrading while the fan is idle.

**Multiple bathrooms:** Use **Apps → Add User App → FOXWARE FoxyFan** again, give the second instance its own label, and select its own sensor and fan switch. Do **not** configure two enabled instances to control the same physical fan.

## Configuration defaults

Defaults reflect the tested webCoRE v2.0.3 Build 20 behavior. Every value below can be adjusted independently per bathroom.

| Setting | Default | What it controls |
| --- | ---: | --- |
| Shower detection rise | **+6 RH points** | Minimum rise above rolling historical baseline to start shower mode |
| RH history window | **5 minutes** | Rolling reference period (minute-sampled history) |
| Minimum shower runtime | **15 minutes** | Drying completion cannot stop the fan earlier |
| Drying allowance | **+9 RH points** | Dry when RH is at or below original shower baseline + allowance |
| Dry confirmation | **5 minutes** | Consecutive time under drying threshold after minimum runtime |
| Maximum shower runtime | **80 minutes** | Hard limit for automatic shower mode |
| FoxyFlatus manual runtime | **20 minutes** | Timed ventilation after manually switching fan ON |
| No-humidity-change warning | **30 minutes** | One-time informational warning when no humidity-change event arrives |
| Allowed location modes | **All modes when none selected** | Optional restriction for *automatic* shower detection only |
| Fan ON/OFF refresh | **Enabled** | Delayed refresh if expected state is not reported and driver supports it |
| Fan control | **Disabled** | Must be enabled explicitly after installation |

**The +6 and +9 thresholds are different:** `+6` starts a shower; `+9` determines when the room has dried sufficiently to *begin* confirmation. The drying comparison uses the humidity baseline saved when the shower was first detected, not the latest rolling value.

**Mode behavior:** A manually activated fan can use FoxyFlatus regardless of the selected location mode. An already-running manual session is **not** automatically converted into a shower session.

## Notifications and instance names

Give each installed app a descriptive name in **Instance naming → Name this FoxyFan instance**. FoxyFan uses that name to identify the originating bathroom in notifications:

```text
FOXWARE FoxyFan (Master Bathroom): Shower detected. RH=74.0%, baseline=66.0%, rise=8.0 points. Fan ON requested.
FOXWARE FoxyFan (Guest Bathroom): Manual FoxFlatus ventilation started; 30-minute timer armed (fan ON event).
```

If an instance has not been renamed, the notification prefix uses its selected fan's name instead. Each notification type has an independent on/off preference; choose one or more Hubitat notification devices to receive them.

## How the controller works

The per-instance controller uses three operating states:

```text
IDLE
 ├─ rapid RH rise + fan OFF + allowed mode ──> SHOWER
 │                                           ├─ minimum runtime
 │                                           ├─ continuous dry confirmation ──> IDLE + OFF request
 │                                           └─ hard maximum timeout ─────────> IDLE + OFF request
 └─ manual fan ON ──────────────────────────> MANUAL
                                             ├─ timer expires ────────────────> IDLE + OFF request
                                             └─ physical fan OFF ─────────────> IDLE
```

Shower ownership is set **before** commanding the fan ON, so its own ON event does not inadvertently trigger FoxyFlatus. Manual timeout callbacks are guarded against stale runs. App-issued OFF requests are reconciled with device reports; a command being sent does not itself prove the hardware switched OFF.

## Tested behavior and known limitations

**Observed on a live Hubitat hub (November 2026):**

- Native manual 2-minute cycles completed with fan OFF reports.
- Manual-start and manual-completion push notifications were received.
- Real humidity rises triggered automatic shower ventilation, and fan ON was confirmed without starting a competing manual timer.
- A full drying cycle respected the minimum runtime and continuous confirmation, then issued fan OFF and received a switch OFF report.
- The `v3.0.4` instance name **Master Bathroom** appeared correctly in shower-start and drying-complete notifications, without duplicate branding.

**Not yet broadly validated:** all third-party sensor/driver combinations, long maximum-runtime timeout scenarios, hub-restart recovery, lost state reports, the early-manual-OFF notification path, and simultaneous operation of two installed bathroom instances. See [`tests/TEST-PLAN.md`](tests/TEST-PLAN.md) for the acceptance matrix.

The application's timers depend on Hubitat scheduling and may fire slightly after the nominal deadline. Fan commands should always be verified against real switch-state reports. This app is for **normal bathroom exhaust automation**, not a safety-critical ventilation system.

## Project contents

```text
FOXWARE-FoxyFan/
├── README.md                     # Public project homepage
├── LICENSE                       # MIT
├── src/FOXWARE-FoxyFan.groovy    # Current app source
├── docs/                         # Design, internal README, changelog, diffs
├── tests/                        # Acceptance plan and static checks
├── reference/                    # Original webCoRE Build 20 reference
└── Releases/
    └── v3.0.0/ ... v3.0.4/       # Frozen source, notes, checksums per release
```

For architecture and historical decisions, read [`docs/DESIGN.md`](docs/DESIGN.md) and [`docs/CHANGELOG.md`](docs/CHANGELOG.md). The [`reference/`](reference/) folder preserves the earlier webCoRE piston for comparison; **the native app does not require webCoRE**.

## Development, bugs, and contributions

Feedback, hardware compatibility reports, and pull requests are welcome once the repository is published. When reporting a problem, please include the Hubitat platform version, device/driver names, relevant event/log timestamps, configuration values, and whether the switch state was physically verified. **Redact hub URLs, public IPs, tokens, mobile-device identifiers, and other personal details.**

This project is provided under the [MIT License](LICENSE), **as-is and without warranty**. Review your fan's electrical/load rating, select appropriate devices, and supervise first-time testing.

---

*FOXWARE FoxyFan — Serious bathroom automation. FoxyFlatus included.* 🦊💨
