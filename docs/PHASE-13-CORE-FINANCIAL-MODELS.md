# Phase 13 - Core financial models

Phase 13 establishes the pure Kotlin financial entities used by persistence, domain rules, sync, and UI layers.

## Exact money representation

`Money` stores values as `Long` minor units. For INR, one major unit contains 100 minor units. No financial
value is stored as `Float` or `Double`, preventing binary floating-point rounding from entering the model.

`Money.fromMajorUnits()` converts text into exact minor units and rejects fractions beyond the currency scale.
Arithmetic uses the JVM exact integer operations so overflow fails immediately instead of silently wrapping.

## Financial entities

- `LedgerTransaction` models expense, income, and transfer records.
- `FinancialAccount` models cash, bank, credit-card, wallet, and other accounts.
- `TransactionCategory` separates expense and income categories.
- `Merchant` provides a stable normalized search key.
- `Budget` models weekly, monthly, and custom spending limits.
- `PaymentMethod` and `TransactionSource` preserve how a record was paid and captured.

## Invariants

- Transaction amounts must be greater than zero.
- Expense and income records require a category.
- Transfers require two different accounts and cannot contain a category or merchant.
- Budget limits must be positive and their end time must follow their start time.
- Identifiers and required names cannot be blank.
- Account opening and current balances must use the same currency.

## Current UI integration

The dashboard totals are represented by `Money`, and draft validation now uses the same exact parser that future
persistence and domain use cases will use.
