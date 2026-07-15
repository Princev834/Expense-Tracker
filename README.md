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
- Phase 6: Code-quality configuration — complete
- Phase 7: GitHub continuous integration — complete
- Phase 8: Design tokens — complete
- Phase 9: Reusable Compose components — complete
- Phase 10: Navigation shell — complete
- Phase 11: Application state architecture — complete
- Phase 12: Logging and error handling — complete
- Phase 13: Core financial models — complete
- Phase 14: Room database foundation — complete
- Phase 15: Repository contracts and local implementations — complete
- Phase 16: Application dependency container — complete
- Phase 17: Default ledger bootstrap — complete
- Phase 18: Manual transaction command — complete
- Phase 19: Transaction entry state and ViewModel — complete
- Phase 20: Real transaction entry screen and navigation — complete
- Phase 21: Atomic account balance updates — complete
- Phase 22: Live Room dashboard — complete

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

## Foundation checkpoints

- Product flavors separate the directly installed personal APK from the future Play Store-safe build.
- Nine Gradle modules establish explicit `core`, `domain`, `feature`, and `platform` boundaries.
- Spotless, ktlint, detekt, compiler-warning enforcement, and a pre-commit hook protect the codebase.
- GitHub Actions runs quality checks, unit tests, and both debug builds on pushes and pull requests.
- The design system now provides centralized colors, typography, shapes, spacing, elevation, and motion tokens.
- Shared buttons, cards, fields, transaction rows, and screen states are available to every feature module.
- A four-destination bottom navigation shell now connects Home, Transactions, Reports, and Settings.
- Lifecycle-aware ViewModels, immutable UI state, actions, loading states, and one-off messages now follow one convention.

## Phase 13

Exact Money values, transactions, accounts, categories, merchants, payment methods, and budget models are active.

## Phase 14

Room now provides the versioned local database, normalized entities, compile-time checked DAOs, schema export, and model mappers.

## Phase 15

Domain repository contracts now isolate features from Room. The database module provides tested local implementations for accounts, categories, merchants, transactions, and budgets.

## Phase 16

The Android application now owns one manual dependency container. The container
provides the privacy-aware logger and the five Room-backed repositories to the
activity and to future feature factories without exposing DAOs or Room entities.

## Phase 17

The application now creates a missing Cash account and practical default expense
and income categories on first launch. The bootstrap is safe to run repeatedly:
existing records are preserved and duplicate defaults are not created.

## Phase 18

A tested domain command can now save manual expenses and income through the
repository boundary. It validates account, category, merchant, currency,
timestamps, and identifier uniqueness before creating a Room transaction.

## Phase 19

The transactions feature now owns a lifecycle-aware ViewModel and immutable form
state. It observes active accounts and matching categories, validates the form,
saves through the Phase 18 command, and exposes one-off user messages.

## Phase 20

Home quick actions now open a dedicated expense or income screen. The form shows
type, amount, account, category, payment method, and note in the correct order,
with the save action after all fields. The temporary dashboard entry fields have
been removed from the visible Home screen.

## Phase 21

Saving an expense now reduces the selected account balance, while saving income
increases it. Room writes the updated account and transaction inside one
database transaction so partial financial state cannot be committed.

## Phase 22

The Home dashboard now observes Room-backed repositories. It displays the total
balance across active accounts, current-month income and expenses, and the five
most recent transactions. Saving through the transaction-entry screen updates
Home automatically when the user returns.
