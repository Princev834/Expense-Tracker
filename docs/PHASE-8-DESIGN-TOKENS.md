# Phase 8 — Design Tokens

Phase 8 establishes the visual contract used by every later Compose screen and reusable component.

## Token groups

- Material 3 dark color scheme
- Finance-specific semantic colors for income, expense, warning, and information states
- Typography scale using the system sans-serif family
- Shape scale for controls, cards, sheets, and large surfaces
- Spacing scale exposed through `MaterialTheme.ledgerSpacing`
- Elevation and motion-duration constants

## Usage rules

Feature modules must use design-system tokens instead of inventing raw colors, spacing values, corner radii, or animation durations. Finance-specific colors are available from `MaterialTheme.ledgerColors`.

Examples:

```kotlin
val spacing = MaterialTheme.ledgerSpacing
val financeColors = MaterialTheme.ledgerColors

Text(
    text = "Income",
    color = financeColors.income,
    modifier = Modifier.padding(spacing.large),
)
```

The application remains dark-only at this stage because that is the selected product direction. A light theme can be added later without changing feature APIs.

## Phase boundary

This phase defines tokens only. Reusable buttons, cards, fields, transaction rows, loading states, and empty states are introduced in Phase 9.
