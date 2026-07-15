# Phase 24 - Live Monthly Reports

Phase 24 replaces the Reports placeholder with a live Room-backed overview.

## Month selection

The report opens on the current local month. Previous-month navigation is
unlimited, while next-month navigation stops at the current month so the user
cannot browse into a future reporting period.

## Exact monthly metrics

The report calculates with the existing exact `Money` model:

- income received;
- expenses paid;
- net cash flow as income minus expenses;
- income and expense transaction count.

Transfers are excluded from these totals because they move money between owned
accounts rather than creating income or spending.

## Expense-category breakdown

Expense transactions are grouped by category and sorted from largest to
smallest. Every row shows:

- category name;
- exact amount;
- percentage of monthly expenses;
- transaction count;
- a proportional visual spending bar.

Historical transactions whose category is no longer active remain visible with
an archived-category fallback label.

## Reactive behavior

The ViewModel observes all transactions and active expense categories. Saving a
transaction updates the selected monthly report automatically without manual
refreshing.

## UI states

The screen supports loading, content, no-activity, income-only, error, and retry
states.

## Architecture and database safety

Reports live in the new `:feature:reports` module. The feature depends only on
domain repository contracts and does not access Room DAOs or entities. Room
schema version 1 remains unchanged.
