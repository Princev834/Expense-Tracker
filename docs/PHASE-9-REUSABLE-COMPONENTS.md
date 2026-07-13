# Phase 9 - Reusable Compose Components

Phase 9 introduces the first reusable UI component layer for Project Ledger.
These components live in `core:designsystem` so future feature modules can share
the same visual behavior without copying Compose code.

## Components

### Buttons

- `LedgerPrimaryButton`
- `LedgerSecondaryButton`

The primary button supports a loading state and prevents duplicate taps while
loading. Both buttons use the shared shape, spacing, typography, and color
tokens.

### Cards

- `LedgerSurfaceCard`
- `LedgerMetricCard`
- `LedgerMetricTone`

Metric cards provide neutral, income, expense, and warning accents for future
dashboard summaries.

### Input fields

- `LedgerTextField`
- `LedgerAmountField`

Both fields expose controlled values, error messages, enabled states, and
consistent finance-app styling. The amount field opens a decimal keyboard.

### Transaction presentation

- `LedgerTransactionRow`
- `LedgerTransactionDirection`

The row supports income, expense, and transfer treatments while keeping amount,
subtitle, and merchant text visually consistent.

### Reusable states

- `LedgerEmptyState`
- `LedgerLoadingState`

These components provide a standard presentation for screens without data and
screens waiting for work to finish.

## Architecture rule

The design-system module contains presentation components only. It does not
contain repositories, ViewModels, transaction calculations, navigation, or
feature-specific business logic.

## Verification

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase9.ps1 -InstallOnPhone
```

The verification performs configuration checks, formatting, static analysis,
module compilation, unit tests, both debug builds, and optional installation of
the personal build.
