# Phase 20 - Real Transaction Entry Screen

Phase 20 makes the transaction-entry flow visible and usable.

## Home navigation

The Home quick actions now navigate to a dedicated form:

- Add expense opens an expense form.
- Add income opens an income form.

The temporary amount and description fields have been removed from the visible
dashboard. Bottom navigation is hidden while the entry form is open, and the
Back action returns to the previous screen.

## Form order

The dedicated screen presents:

1. expense or income selector;
2. amount;
3. account;
4. category;
5. payment method;
6. optional note;
7. save action.

The save button therefore appears only after the fields it submits.

## State connection

`TransactionEntryRoute` creates the Phase 19 ViewModel through the application
factory and collects its state with lifecycle awareness. Identified messages are
displayed through a Snackbar and consumed only after display.

## Navigation and deep links

The application adds a hidden navigation destination:

- `transaction-entry/expense`
- `transaction-entry/income`

The matching development deep links are:

- `projectledger://entry/expense`
- `projectledger://entry/income`

## Database safety

The screen saves through the Phase 18 command and existing repositories. Room
schema version 1 remains unchanged.
