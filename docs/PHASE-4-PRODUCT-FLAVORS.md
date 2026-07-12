# Phase 4 — Product Flavors

## Purpose

This phase creates a hard build-time boundary between the private personal APK and a future Play Store-safe build.

## Flavor matrix

| Variant | Application ID | App label | Restricted SMS permissions |
|---|---|---|---|
| `personalDebug` | `com.princevekariya.projectledger.personal.debug` | Project Ledger Personal Dev | Included |
| `personalRelease` | `com.princevekariya.projectledger.personal` | Project Ledger Personal | Included |
| `playDebug` | `com.princevekariya.projectledger.debug` | Project Ledger Play Dev | Excluded |
| `playRelease` | `com.princevekariya.projectledger` | Project Ledger | Excluded |

## Why this boundary exists

The personal APK is installed directly and may later read transaction SMS messages after explicit runtime permission approval. The Play Store build intentionally does not declare `READ_SMS` or `RECEIVE_SMS`.

Phase 4 only establishes build identities, capability flags, labels, and manifest separation. It does **not** read SMS messages yet. Permission education, runtime requests, import, parsing, and live detection are implemented in later automation phases.

## BuildConfig flags

Every variant exposes:

- `DISTRIBUTION_CHANNEL`
- `SMS_AUTOMATION_AVAILABLE`
- `PLAY_STORE_SAFE`
- `APP_ENVIRONMENT`
- `ENABLE_VERBOSE_LOGGING`

Application code accesses distribution information through `CurrentAppVariant` instead of scattering raw string checks throughout the codebase.

## Build commands

```powershell
.\gradlew.bat clean `
  testPersonalDebugUnitTest `
  testPlayDebugUnitTest `
  assemblePersonalDebug `
  assemblePlayDebug `
  assemblePersonalRelease `
  assemblePlayRelease `
  --no-daemon --max-workers=1 --console=plain
```

## Android Studio Build Variants

Open **Build → Select Build Variant** or the **Build Variants** tool window. Use `personalDebug` for normal development on the private app. Use `playDebug` only when verifying the store-safe build.
