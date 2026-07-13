# Phase 10 — Navigation Shell

Phase 10 introduces the first real application shell around the existing feature screens.

## Destinations

- Home
- Transactions
- Reports
- Settings

The Home destination hosts the existing reusable-component dashboard. Transactions is owned by the existing
`feature:transactions` module. Reports and Settings are temporary placeholders until their dedicated feature
modules are introduced.

## Navigation behavior

The bottom navigation uses Navigation Compose with state restoration:

- Re-selecting the current destination does not add another copy.
- Switching tabs saves and restores destination state.
- The first destination remains the navigation graph start destination.
- Back navigation follows the Navigation Compose back stack.

## Deep-link placeholders

The personal and Play variants both understand these private application links:

- `projectledger://home`
- `projectledger://transactions`
- `projectledger://reports`
- `projectledger://settings`

These custom-scheme links are development placeholders. Public verified web links will be configured only after
the final product domain and application identity are selected.

## Architecture decision

Navigation wiring stays in the `app` module because it composes multiple feature modules. Feature modules expose
screens but do not depend on the application shell. This keeps feature-to-feature dependencies out of the graph.

## Verification

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase10.ps1 -InstallOnPhone
```
