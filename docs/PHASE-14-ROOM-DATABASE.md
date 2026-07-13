# Phase 14 - Room database foundation

Phase 14 introduces the versioned offline database used by all future transaction, budget, account, and report flows.

## Module boundary

`core:database` is an Android library. It exposes the pure financial models used by its public mapper functions while
keeping Room-specific entities and DAOs inside the persistence layer. The app creates one database instance for the
process through `ProjectLedgerApplication`.

## Schema version 1

The first schema contains five normalized tables:

- `financial_accounts`
- `transaction_categories`
- `merchants`
- `ledger_transactions`
- `budgets`

Money is stored as `Long` minor units plus an explicit currency code. Enum values are stored using stable names. Every
foreign-key column has an index, and transactions are indexed by occurrence time and transaction type.

## Data access

Each table has a dedicated DAO. Read streams return Kotlin `Flow`, while writes use suspending `@Upsert` operations.
Room validates every SQL query during compilation.

## Migration discipline

Schema export is enabled and written to `core/database/schemas`. Version changes must include a reviewed migration.
Destructive migration fallback is intentionally not configured because personal finance data must never be silently
removed.

## Current application integration

The database instance is created at application startup so missing Room-generated code is detected by the existing
startup smoke test. Transaction persistence and repository orchestration are intentionally deferred to the next phase.
