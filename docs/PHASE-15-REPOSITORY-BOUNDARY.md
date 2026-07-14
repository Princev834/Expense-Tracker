# Phase 15 - Repository Boundary

Phase 15 separates domain-facing data access from Room implementation details.

## Domain contracts

The `domain:transactions` module exposes repository interfaces for:

- financial accounts;
- transaction categories;
- merchants;
- ledger transactions;
- budgets.

These contracts contain only core financial models, Kotlin coroutines, and `Flow`.
They do not expose Android contexts, Room entities, DAOs, or SQL details.

## Local implementations

The `core:database` module implements every contract with the Room DAOs created
in Phase 14. Entities are converted to and from validated domain models at the
repository boundary.

A `LedgerRepositories` container constructs all five local repositories from one
`ProjectLedgerDatabase` instance. The application composition root will consume
this container in the next data-wiring phase.

## Safety rules

- Public repository APIs expose their model and coroutine dependencies through
  Gradle `api` dependencies.
- Blank identifiers are rejected before DAO lookups and deletions.
- Recent transaction limits must be greater than zero.
- Merchant search keys are normalized consistently.
- Database schema version 1 remains unchanged.
- No destructive migration fallback is introduced.
