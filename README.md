# Project Ledger

Project Ledger is an offline-first Android personal finance application being built from scratch with Kotlin and Jetpack Compose.

> The product name and package identity are temporary during the foundation phases and can be changed before release configuration.

## Current status

- Phase 0: Architecture and roadmap — complete
- Phase 1: Development environment — complete
- Phase 2: Repository and starter Android project — complete
- Phase 3: Gradle build convention — complete
- Phase 4: Personal and Play product flavors — complete
- Phase 5: Initial multi-module structure — in verification

## Development baseline

- Android Studio Jellyfish
- Kotlin 1.9.24
- Android Gradle Plugin 8.4.2
- Gradle 8.6
- JDK 17
- Jetpack Compose
- compileSdk 34
- targetSdk 34
- minSdk 26

## Open the project

Open the folder containing `settings.gradle.kts` in Android Studio. Allow Gradle synchronization to finish, select an authorized Android device, and run the `app` configuration.

## Temporary application identity

- Application ID: `com.princevekariya.projectledger`
- Display name: `Project Ledger`

Both can be changed later when final branding is chosen.

## Build commands

Windows PowerShell:

```powershell
.\gradlew.bat clean test assembleDebug
```

Install the debug APK on a connected phone:

```powershell
.\gradlew.bat installDebug
```

## Repository policy

Do not commit local SDK paths, signing keys, environment files, service credentials, or production secrets. The included `.gitignore` excludes these files.

## Current development checkpoint

- Phase 0: architecture and roadmap — complete
- Phase 1: environment setup — complete
- Phase 2: project initialization — complete
- Phase 3: Gradle build convention — complete

The development build uses the separate package `com.princevekariya.projectledger.debug` and appears on Android as **Project Ledger Dev**.

<!-- PHASE-4-CHECKPOINT -->
## Phase 4 checkpoint

Product flavors now separate the directly installed personal APK from the future Play Store-safe build. Normal development uses `personalDebug`; the `play` flavor excludes restricted SMS permissions.


<!-- PHASE-5-CHECKPOINT -->
## Phase 5 checkpoint

The project now uses eight Gradle modules with explicit `core`, `domain`, `feature`, and `platform` boundaries. The app module is a thin composition root, the Compose theme lives in `:core:designsystem`, and the visible foundation screen lives in `:feature:dashboard`.
