# Phase 5 — Initial Module Structure

## Purpose

Phase 5 changes Project Ledger from a single Android application module into a controlled multi-module project. The app module is now only the composition root: it selects the build variant, reads Android platform information, applies the design system, and opens a feature screen.

## Modules introduced

| Module | Type | Responsibility |
|---|---|---|
| `:app` | Android application | Composition root, manifests, flavors and final APK |
| `:core:common` | Pure Kotlin | Stable cross-project constants and future utilities |
| `:core:model` | Pure Kotlin | Shared immutable models used across layers |
| `:core:designsystem` | Android library | Shared Compose theme and later reusable UI components |
| `:domain:transactions` | Pure Kotlin | Future transaction rules and use cases without Android dependencies |
| `:feature:dashboard` | Android library | Dashboard presentation and Compose UI |
| `:feature:transactions` | Android library | Boundary for transaction screens added in later phases |
| `:platform:device` | Android library | Android-specific device information |

## Dependency direction

```text
app
├── feature:dashboard
├── core:model
├── core:designsystem
└── platform:device

feature:dashboard
├── core:model
└── core:designsystem

feature:transactions
├── core:model
├── core:designsystem
└── domain:transactions

domain:transactions
├── core:common
└── core:model
```

Rules:

1. `core` never depends on `feature`, `platform`, or `app`.
2. `domain` is pure Kotlin and never imports Android APIs.
3. `feature` modules can use core and domain modules but never import the app module.
4. Android-specific implementations stay in `platform` modules.
5. `app` is the only module allowed to assemble the final application and connect implementations.

## Why only these modules exist now

The final architecture contains more modules, but creating every future empty module immediately would increase Gradle configuration time and memory use on the current laptop. New database, network, automation, reporting, and settings modules will be introduced only when their implementation phase begins.

This keeps module boundaries meaningful while maintaining acceptable build performance.

## MainActivity responsibility

`MainActivity` no longer contains the screen implementation or theme definitions. It now acts as a small composition root and passes configuration into `:feature:dashboard`.

## Verification

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase5.ps1 -InstallOnPhone
```

The script validates module declarations, dependency direction, independent module compilation, both debug flavors, and installation of the personal debug APK.
