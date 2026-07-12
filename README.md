# Project Ledger

Project Ledger is an offline-first Android personal finance application being built from scratch with Kotlin and Jetpack Compose.

> The product name and package identity are temporary during the foundation phases and can be changed before release configuration.

## Current status

- Phase 0: Architecture and roadmap — complete
- Phase 1: Development environment — complete
- Phase 2: Repository and starter Android project — complete
- Phase 3: Gradle build convention — complete
- Phase 4: Personal and Play product flavors — complete
- Phase 5: Initial multi-module structure — complete
- Phase 6: Code-quality configuration — in verification

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

Open the folder containing `settings.gradle.kts` in Android Studio. Allow Gradle synchronization to finish, select an authorized Android device, and run the `app` configuration with the `personalDebug` build variant.

## Temporary application identity

- Application ID: `com.princevekariya.projectledger`
- Personal debug ID: `com.princevekariya.projectledger.personal.debug`
- Display name: `Project Ledger`

Both can be changed later when final branding is chosen.

## Build commands

Build the personal development APK:

```powershell
.\gradlew.bat :app:assemblePersonalDebug --no-daemon --max-workers=1
```

Run the repository quality gate:

```powershell
.\gradlew.bat qualityCheck --no-daemon --max-workers=1
```

Apply automatic formatting:

```powershell
.\gradlew.bat qualityFix --no-daemon --max-workers=1
```

## Repository policy

Do not commit local SDK paths, signing keys, environment files, service credentials, or production secrets. The included `.gitignore` excludes these files. A repository-managed pre-commit hook blocks commits that fail formatting or static analysis.

<!-- PHASE-4-CHECKPOINT -->
## Phase 4 checkpoint

Product flavors separate the directly installed personal APK from the future Play Store-safe build. Normal development uses `personalDebug`; the `play` flavor excludes restricted SMS permissions.

<!-- PHASE-5-CHECKPOINT -->
## Phase 5 checkpoint

The project uses eight Gradle modules with explicit `core`, `domain`, `feature`, and `platform` boundaries. The app module is a thin composition root, the Compose theme lives in `:core:designsystem`, and the visible foundation screen lives in `:feature:dashboard`.

<!-- PHASE-6-CHECKPOINT -->
## Phase 6 checkpoint

Spotless, ktlint, detekt, compiler warning enforcement, and a Git pre-commit quality gate now protect the codebase before later feature development begins.
