# FOXWARE FoxyFan v3.0.3 — Release Notes

**Release date:** 2026-11-23
**Type:** Cosmetic UI-only update
**Baseline:** v3.0.2 (the running native Hubitat app)

## Changes from v3.0.2

- Replace the activation setting's label **"ENABLE native fan control (leave OFF until cutover)"** with **"Enable Fan Control (can be disabled for testing)"**.
- Update the application title and installation log to report **v3.0.3**.
- Update source-code header with confirmed real-world validation status. No executable behavior changed.

## Unchanged behavior

- Preference key remains **`enableControl`**: existing installations retain their saved enabled/disabled setting.
- New installations still default to **OFF** as a safe default.
- All fan state handling, shower detection/drying logic, FoxFlatus timer, push notification settings, refresh callbacks, and configured device settings are unchanged.
- All previous versioned release directories are preserved unmodified.

## Upgrade

1. With the fan idle, open **Hubitat → Apps Code → FOXWARE FoxyFan**.
2. Replace the source with `FOXWARE-FoxyFan-v3.0.3.groovy` and click **Save**.
3. Open the existing installed FoxyFan app and confirm the new page title and activation label. Click **Done** if needed to save configuration/reinitialize.
4. Verify your existing **Enable Fan Control** setting is still ON if you intend the automation to continue running.

**No uninstall/reinstall or new app instance is needed.** Do not enable the old webCoRE controller simultaneously.

## Validation

- v3.0.2: user-provided live logs confirmed manual timed OFF, the optional post-OFF refresh, shower start and digital ON reporting, dry-confirmation start and successful drying-complete OFF at 18:46:32 on November 23, 2026. Push screenshots confirmed the manual start and completion notifications and shower-start notification.
- v3.0.3: source/release byte parity, version-label diff, static structure, SHA-256 manifest and ZIP integrity checked locally.
- v3.0.3 has **not** yet been compiled/saved on the hub. No fan control changes require retesting of the full shower cycle, though checking the new UI label is recommended.
