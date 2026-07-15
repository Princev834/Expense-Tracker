# Phase 23 - Live Transaction History

Phase 23 replaces the Transactions placeholder with a real Room-backed screen.

## Live source data

The history ViewModel combines repository flows for:

- financial accounts;
- ledger transactions;
- expense categories;
- income categories;
- merchants.

The screen refreshes automatically whenever any source changes.

## Transaction rows

Transactions are ordered by occurrence time and creation time, newest first.
Each visible row contains:

- merchant, note, category, or fallback title;
- category or transaction type;
- account name;
- local date;
- payment method;
- signed and formatted amount.

## Filters

The top filter chips provide:

- All
- Expense
- Income

Filtering is performed against the latest in-memory repository snapshot and
does not restart the database observation.

## UI states

The screen supports real loading, content, empty-filter, error, and retry
states. The bottom navigation remains visible because transaction history is a
primary application destination.

## Database safety

Phase 23 is read-only. Room schema version 1 and all existing write behavior
remain unchanged.
