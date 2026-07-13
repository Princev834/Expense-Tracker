# Phase 12 - Logging and error handling

Phase 12 establishes privacy-aware diagnostic logging and predictable error presentation.

## Logging rules

- Debug and information logs are enabled only when the build enables verbose logging.
- Warning and error logs remain available in every build.
- Email addresses, UPI identifiers, and long numeric references are redacted.
- Release builds do not attach throwable details to Logcat messages.
- Transaction descriptions and amounts are never written by dashboard validation logs.

## Error rules

`Throwable.toUserFacingError()` maps technical exceptions to stable user-readable guidance.
Internal exception messages are not displayed directly to users.

## Process reporting

`AndroidProcessErrorReporter` records an unexpected uncaught failure and then delegates to
Android's existing uncaught-exception handler. It does not suppress or restart a crashed process.

## Validation behavior

The dashboard validates description and amount fields before future transaction persistence is
connected. Invalid drafts produce safe Snackbar guidance and structured warning events.
