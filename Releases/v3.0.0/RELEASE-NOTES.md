# FOXWARE FoxyFan v3.0.0 — Native Hubitat Edition

**2026-11-23 · FIRST CODING DRAFT · NOT PRODUCTION RELEASE**

## Overview

A single-file, configurable **Hubitat Groovy app**, modeled on FoxyFan's proven webCoRE v2.0.3 Build 20. Multiple independent installed app instances support different bathrooms without modifying the source. This is a development artifact for controlled testing, **not yet confirmed to compile or behave correctly on a Hubitat hub**.

## Defaults

Shower trigger +6 RH points; history five minutes; minimum runtime 15 minutes; drying threshold baseline +9 points; five-minute dry confirmation; maximum 80 minutes; manual FoxFlatus timer 20 minutes. All configurable. Allowed modes default to unrestricted until selected; to mirror Build 20, select the three existing Home modes.

## Highlights

- Select humidity sensor and fan switch per instance (capability-based selectors).
- Native push notification device(s), independently configurable messages.
- Safe-by-default native fan control **OFF** until intentional cutover.
- Single-threaded, one-controller state machine plus generation-guarded manual timeout.
- Preserves B20's fan-ownership-before-fan-ON ordering, and prevents stale ON rearm after OFF.
- Avoids double completion on simultaneous shower dry and maximum timeouts.
- Uses Switch-only on/off for broader device compatibility, without requiring Refresh.

## Installation

See `../../docs/README.md`, especially the safety cutover and rollback sections, before touching production. For manual pasting into Hubitat **Apps Code**, use `FOXWARE-FoxyFan-v3.0.0.groovy`. Verify the `SHA256SUMS` file if desired. Run `../../tests/TEST-PLAN.md` against your actual hub before production use.

## Known limitations

- Not tested on any hub, including actual Hubitat app compilation, scheduler behavior, and Groovy sandbox restrictions.
- No auto-promotion from MANUAL to SHOWER when fan already ON (intentional B20 parity).
- Notification devices must be explicitly selected to receive alerts.
- Sensor update frequency is driver/device dependent; static RH can also trigger stale-change warnings.
- Changes to runtime settings during an active cycle apply immediately to deadlines; timestamp/ownership persists for unchanged devices.
- Existing webCoRE `@humidtrip` global is intentionally not written.
- Do not run this app and webCoRE piston together on one switch; no cross-installation interlock exists for duplicate device selections.
