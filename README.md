# Project Ledger

Project Ledger is an offline-first Android personal finance application being built from scratch with Kotlin and Jetpack Compose.

> The product name and package identity are temporary during the foundation phases and can be changed before release configuration.

## Current status

- Phase 0: Architecture and roadmap — complete
- Phase 1: Development environment — complete
- Phase 2: Repository and starter Android project — in verification

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

<!-- PHASE-3-CHECKPOINT -->
## Current development checkpoint

- Phase 0: architecture and roadmap â€” complete
- Phase 1: environment setup â€” complete
- Phase 2: project initialization â€” complete
- Phase 3: Gradle build convention â€” complete

The development build uses the separate package `com.princevekariya.projectledger.debug` and appears on Android as **Project Ledger Dev**.
