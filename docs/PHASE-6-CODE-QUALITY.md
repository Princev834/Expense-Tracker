# Phase 6 — Code Quality Configuration

Phase 6 establishes a repeatable code-quality gate for every module in Project Ledger.

## Added tools

- **Spotless 6.25.0** manages deterministic formatting.
- **ktlint 1.2.1** formats Kotlin and Kotlin Gradle files using the Android Studio style.
- **detekt 1.23.6** performs Kotlin static analysis with repository-owned rules.
- Kotlin compiler warnings are treated as build errors through `warningsAsErrors=true`.
- A repository-managed Git pre-commit hook runs the quality gate before each commit.

## Main commands

Check formatting and static analysis:

```powershell
.\gradlew.bat qualityCheck --no-daemon --max-workers=1
```

Automatically format supported files:

```powershell
.\gradlew.bat qualityFix --no-daemon --max-workers=1
```

Run the Windows helper:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-QualityChecks.ps1
```

Apply formatting and then check:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-QualityChecks.ps1 -ApplyFormatting
```

## Git pre-commit gate

The repository uses:

```text
.githooks/pre-commit
```

The apply script configures:

```powershell
git config core.hooksPath .githooks
```

A commit is blocked when formatting or detekt checks fail. The hook does not modify files automatically; run `qualityFix`, review the changes, and commit again.

## Configuration locations

```text
.editorconfig
config/detekt/detekt.yml
gradle/libs.versions.toml
build.gradle.kts
gradle.properties
```

## Resource-conscious execution

The verification scripts use `--no-daemon` and `--max-workers=1` to remain stable on the current development laptop.
