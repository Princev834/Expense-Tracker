# Phase 3 — Gradle Build Convention

## Purpose

This phase centralizes dependency and plugin versions and establishes separate development and production build behavior.

## Files introduced or changed

- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/princevekariya/projectledger/MainActivity.kt`
- `app/src/test/java/com/princevekariya/projectledger/ProjectInitializationTest.kt`

## Version catalog

All plugin and dependency versions now live in `gradle/libs.versions.toml`. Build scripts reference type-safe aliases such as:

```kotlin
alias(libs.plugins.android.application)
implementation(libs.androidx.core.ktx)
```

This prevents versions from being repeated across future modules.

## Debug build

- Application ID: `com.princevekariya.projectledger.debug`
- App label: `Project Ledger Dev`
- Version suffix: `-debug`
- Minification: disabled
- Verbose logging flag: enabled
- Environment: `development`

The separate application ID allows a development build and a future production build to coexist on the same Android phone.

## Release build

- Application ID: `com.princevekariya.projectledger`
- App label: `Project Ledger`
- Minification: enabled
- Resource shrinking: enabled
- Verbose logging flag: disabled
- Environment: `production`

The release APK is not signed in this phase. Release signing will be configured during the release milestone.

## Verification commands

```powershell
.\gradlew.bat --stop
.\gradlew.bat clean testDebugUnitTest assembleDebug assembleRelease --no-daemon --max-workers=1
```

Expected outputs:

```text
app\build\outputs\apk\debug\app-debug.apk
app\build\outputs\apk\release\app-release-unsigned.apk
```
