# Phase 21 - Atomic Account Balance Updates

Phase 21 makes manual transactions affect the selected financial account.

## Balance projection

`AccountBalanceProjector` applies exact `Money` arithmetic:

- expense subtracts the amount from the current balance;
- income adds the amount to the current balance;
- opening balance remains unchanged;
- transfer is rejected because it requires two account updates.

Negative balances are preserved when valid arithmetic produces them. This is
important for bank, credit, and cash records that may temporarily be below zero.

## Atomic persistence

`TransactionRepository.saveWithUpdatedAccount` stores:

1. the updated financial account;
2. the new ledger transaction.

`RoomTransactionRepository` executes both DAO writes through
`RoomDatabase.withTransaction`. When either write fails, Room rolls back both.

## Safety checks

The persistence boundary confirms that:

- the transaction is not a transfer;
- the updated account matches the transaction account identifier;
- transaction and account currencies match.

## Existing schema

Phase 21 changes behavior only. Room schema version 1 and all five existing
tables remain unchanged.
