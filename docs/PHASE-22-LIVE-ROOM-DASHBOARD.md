# Phase 22 - Live Room Dashboard

Phase 22 removes the remaining dashboard demo values and connects Home to the
offline Room data already managed by the repository layer.

## Live metrics

The dashboard observes active accounts and all transactions to calculate:

- total current balance across active accounts;
- income during the current local calendar month;
- expenses during the current local calendar month;
- number of active accounts.

All totals use exact `Money` arithmetic.

## Recent activity

The five newest transactions are mapped into dashboard rows. The title prefers:

1. merchant name;
2. transaction note;
3. category name;
4. a safe expense, income, or transfer fallback.

The subtitle includes the category or transaction type and a local date label.

## Reactive updates

`DashboardViewModel` combines account, transaction, category, and merchant
repository flows. A successful transaction from the entry screen changes the
Room account and transaction rows atomically, and the existing Home ViewModel
receives those changes without manual refresh.

## Loading and errors

Home now has genuine loading, content, empty, and error states. Retry restarts
the repository observation after a failure.

## Database schema

Phase 22 reads existing repositories only. Room schema version 1 and all five
tables remain unchanged.
