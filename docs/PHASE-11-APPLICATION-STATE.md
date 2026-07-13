# Phase 11 - Application State Architecture

Phase 11 establishes the state-management convention used by every interactive feature.

## Screen contract

Each stateful screen is divided into these responsibilities:

1. **Route** obtains the ViewModel and collects state with lifecycle awareness.
2. **ViewModel** owns mutable state and processes user actions.
3. **UiState** is an immutable snapshot containing everything required to render the screen.
4. **Action** represents input from the user or the UI layer.
5. **Screen** is stateless and renders only the supplied state and callbacks.

The flow is one-way:

```text
User action -> ViewModel -> new UiState -> Compose render
```

## Shared state types

`core:common` now contains:

- `UiLoadState` for idle, loading, content, and error states.
- `UiMessage` for identified one-off user messages.

These types contain no Android dependency and can be reused by all feature modules.

## Lifecycle handling

`DashboardRoute` uses `collectAsStateWithLifecycle()` so collection automatically follows the
Android lifecycle. The ViewModel survives configuration changes, while the composable screen
remains stateless.

## One-off messages

The ViewModel emits an identified `UiMessage`. The route displays it through a Snackbar and sends
`MessageShown` back to the ViewModel. Matching identifiers prevent an older callback from clearing
a newer message.

## Future feature rule

New stateful features should use the same naming pattern:

```text
FeatureRoute
FeatureViewModel
FeatureUiState
FeatureAction
FeatureScreen
```

Business logic must not be placed inside composable functions.
