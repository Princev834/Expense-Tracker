# Phase 7 — Continuous Integration

Phase 7 adds a GitHub Actions workflow that verifies every push and pull request.

## What the workflow checks

- Kotlin formatting with Spotless and ktlint
- Static analysis with Detekt
- Compiler warnings as errors
- Core and domain unit tests
- Personal debug unit tests
- Play-safe debug unit tests
- Personal debug APK build
- Play-safe debug APK build

## Workflow file

```text
.github/workflows/android-ci.yml
```

## Triggers

The workflow runs for:

- Pushes to `main`, `master`, or `develop`
- Pull requests targeting `main`, `master`, or `develop`
- Manual runs from the GitHub Actions tab

## Security and reliability

The workflow uses read-only repository permissions. It does not use project secrets,
production signing keys, SMS data, cloud credentials, or a connected Android phone.

The two debug APKs are uploaded as temporary workflow artifacts after a successful run.
Diagnostic reports are uploaded only after failure.

## Local verification

From the project root, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase7.ps1
```

## GitHub verification

After the local verification succeeds:

```powershell
git status
git add .
git commit -m "ci: add Android quality and build workflow"
git push
```

Then open the repository on GitHub and select:

```text
Actions → Android CI
```

The latest run must finish with a green check before Phase 7 is considered complete.
