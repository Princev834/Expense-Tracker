# Phase 16 - Application Dependency Container

Phase 16 establishes a single application-level composition root.

## Container responsibilities

`AppContainer` exposes only:

- the shared privacy-aware `AppLogger`;
- the five repositories grouped by `LedgerRepositories`.

The container does not expose Room DAOs, entities, SQL queries, or Android
activities to feature modules.

## Application startup

`ProjectLedgerApplication` now:

1. creates the Android logger;
2. installs the process-level error reporter;
3. creates the Room database;
4. converts the database DAOs into repository implementations;
5. stores those dependencies in `DefaultAppContainer`.

`MainActivity` resolves its logger from the application container instead of
reading separate mutable application properties.

## Why manual dependency injection

The project currently needs only one process-level dependency graph. A small
manual container keeps the dependency lifecycle explicit and avoids introducing
another annotation processor or runtime framework before the feature graph
requires one.

## Verification

The Phase 16 device smoke test requires the process to remain alive and checks
Logcat for `application_container_ready`. Verification therefore fails when the
container cannot initialize even when APK installation itself succeeds.
