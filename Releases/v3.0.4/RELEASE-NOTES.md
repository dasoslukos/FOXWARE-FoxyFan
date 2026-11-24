# FOXWARE FoxyFan v3.0.4 — Release Notes

**Release date:** 2026-11-23
**Type:** Notification identity and native per-instance naming UI
**Baseline:** v3.0.3 (native v3.0.2 manual and full shower cycles already tested)

## Added native app-instance naming control

The configuration page now opens with **Instance naming → Name this FoxyFan instance (e.g., Master Bathroom)**. This uses Hubitat's built-in `label` preference, so the name is the actual **installed app label** (`app.label`) shown in the Hubitat Apps list. It is not a second, potentially inconsistent custom-name setting.

- Name one installation `Master Bathroom` and another `Guest Bathroom`.
- Push notifications reuse the existing per-instance `notificationIdentity()` formatter; e.g., `FOXWARE FoxyFan (Master Bathroom): ...`.
- Existing installed app labels are preserved until explicitly changed. An optional blank/default label still falls back to the selected fan's name for notification identity.
- Name changes should be made while the fan is idle; click **Done** to save and refresh the status/identity preview.

## User-visible fix

- Before: `FOXWARE FoxyFan (FOXWARE FoxyFan): Shower detected...`
- After, default-named Master Bath app: `FOXWARE FoxyFan (Master Bath Fan): Shower detected...`
- After, installed app renamed `Guest Bathroom`: `FOXWARE FoxyFan (Guest Bathroom): Shower detected...`
- After, an already branded installed app named `FOXWARE FoxyFan - Guest Bath`: `FOXWARE FoxyFan - Guest Bath: Shower detected...`

A **Notification identity** preview is shown under **Activation and instance status**. The full prefix is built once via `notificationIdentity()` and reused by all six push paths: manual start, manual completion, manual early OFF, shower start, drying complete, automatic maximum.

## Unchanged

All fan commands, humidity detection, timers, configuration values, enabled settings, 3-second post-OFF/ON refresh logic, status arbitration, and stored preference keys remain unchanged. No additional mobile notification devices or notification toggles are introduced. A Hubitat installed-app label may be customized for each bathroom. The selected fan device name is used when the installed-app label is unchanged. The new native instance naming section is the **only UI addition**.

## Upgrade / verify

1. Prefer updating while fan is idle. Keep old webCoRE pistons disabled.
2. Hubitat **Apps Code → FOXWARE FoxyFan**: replace source with `FOXWARE-FoxyFan-v3.0.4.groovy`; click **Save**.
3. Reopen each installed FoxyFan instance and check version 3.0.4 and **Notification identity**; click **Done** to refresh settings/subscriptions. Existing saved settings are retained.
4. In the new **Instance naming** section set e.g. `Master Bathroom`, click **Done**, and confirm Hubitat Apps list and Notification identity preview show the new name. Trigger a short manual ON test with its start push enabled. Verify a **single, distinct** prefix for that bathroom. The previous two-minute manual and full shower cycles need not be repeated solely for this formatting change, though routine regression checks are encouraged.
5. For a second bathroom, install a separate instance with distinct fan and humidity devices and set its own name in the **Instance naming** section.

## Validation / limits

- Static source invariants, native label-control presence, unchanged control path, previous-release preservation, release/source byte parity, SHA-256 checksums, and package integrity verified locally.
- v3.0.2 manual and full automatic shower/drying operation were verified from user-provided live Hubitat logs on November 23, 2026; v3.0.4 formatting and the new built-in instance-naming field still await live Hubitat validation.
- Local checks do not compile Groovy in Hubitat or exercise the phone notification service.
