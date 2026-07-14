# Phase 18 - Manual Transaction Command

Phase 18 adds the domain operation that future entry screens will call.

## Draft

`ManualTransactionDraft` represents an expense or income before persistence.
It contains:

- transaction type;
- exact `Money` amount;
- account and category identifiers;
- payment method;
- optional occurrence time;
- optional merchant search key;
- optional note.

Transfer creation remains separate because it requires two accounts and
different accounting rules.

## Validation before persistence

`SaveManualTransactionUseCase` confirms that:

- the account exists and is active;
- the amount currency matches the account;
- the category exists, is active, and matches expense or income;
- an optional merchant exists and is active;
- generated transaction identifiers are nonblank and unique;
- created and updated timestamps come from an injected time provider.

Only after those checks does it call `TransactionRepository.save`.

## Testability

The use case receives a transaction ID generator and epoch-time provider.
Production uses UUID and system-time implementations, while unit tests use
deterministic values.

## Current scope

Phase 18 creates and persists transactions but does not yet expose the command
through the temporary dashboard form. The proper transaction-entry UI and its
ViewModel will be introduced in the next feature phases.

Account balance projection will also be implemented separately so transaction
persistence and balance calculations remain testable and consistent.
