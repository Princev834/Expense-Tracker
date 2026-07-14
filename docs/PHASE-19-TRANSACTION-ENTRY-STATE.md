# Phase 19 - Transaction Entry State

Phase 19 creates the lifecycle-aware state holder for the real expense and
income entry feature.

## State

`TransactionEntryUiState` contains:

- expense or income selection;
- amount and note input;
- selected account and category;
- payment method;
- active account and category choices;
- reference-loading and saving status;
- identified one-time messages.

`canSave` is derived from the immutable state and exact `Money` parsing.

## ViewModel

`TransactionEntryViewModel`:

- observes active accounts;
- observes categories matching the selected transaction type;
- selects the first valid account and category when necessary;
- rejects unavailable selections;
- limits amount and note input lengths;
- calls `SaveManualTransactionUseCase`;
- clears editable fields after a successful save;
- converts failures into stable user-facing messages;
- avoids writing entered financial values into logs.

## Factory

The application container owns `TransactionEntryViewModelFactory`. This keeps
repository and command construction inside the application composition root.

## Current scope

Phase 19 does not replace the temporary dashboard form. The next transaction
feature phases will add the proper entry route and Compose screen, with fields
first and the save action at the bottom.
