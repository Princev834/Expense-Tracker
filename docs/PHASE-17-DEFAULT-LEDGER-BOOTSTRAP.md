# Phase 17 - Default Ledger Bootstrap

Phase 17 prepares the local database for the first real transaction.

## Starter data

The application guarantees that the following records exist:

- one Cash account;
- eight expense categories;
- four income categories.

The categories cover common student spending such as food, transport,
education, recharge, shopping, health, entertainment, and pocket money.

## Idempotency

`EnsureDefaultLedgerDataUseCase` checks each stable identifier before saving it.

This means:

- the bootstrap can run on every application launch;
- duplicate default records are not created;
- an existing record with a default identifier is never overwritten;
- application updates remain safe for existing users.

## Startup behavior

The application container owns the bootstrap use case. The application runs it
on an IO coroutine after the logger, database, and repositories are ready.

A successful run emits `default_ledger_data_ready`. A failure is logged through
the privacy-aware logger and does not terminate the application process.

## Database schema

Phase 17 inserts rows through the existing repositories. Room schema version 1
and all five existing tables remain unchanged.
