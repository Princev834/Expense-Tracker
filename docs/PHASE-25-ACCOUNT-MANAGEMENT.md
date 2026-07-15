# Phase 25 - Account Management

Phase 25 replaces the Settings placeholder with live Room-backed account
management.

## Account list

The screen observes the account repository and displays every financial account
with:

- account name;
- account type;
- current balance;
- opening balance;
- active or archived status.

The list is ordered with active accounts first and then alphabetically.

## Account creation

The inline form accepts:

- account name;
- opening balance;
- cash, bank account, credit card, digital wallet, or other type.

`CreateFinancialAccountUseCase` trims and validates the name, rejects duplicate
names case-insensitively, generates a unique identifier, and initializes both
opening and current balances with the exact `Money` value.

## Reactive integration

Saving through `AccountRepository` updates the Settings list immediately. The
same repository flow also feeds transaction entry and the dashboard, so the new
account becomes available without restarting the application.

## UI states

The screen supports loading, content, empty, error, retry, form validation,
saving, cancellation, and one-off success or failure messages.

## Architecture and schema safety

Account management lives in the new `:feature:settings` module. Domain account
creation remains independent of Android and Room. Room schema version 1 is
unchanged.
