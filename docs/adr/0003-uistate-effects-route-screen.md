# 0003. Single UiState, Channel effects and Route/Screen split

Status: Accepted

## Context

Screens that take a ViewModel directly cannot be previewed or tested without Hilt. Events modelled as state are replayed after rotation (for example opening a file twice), and several parallel flows per screen make partial states possible.

## Decision

For each screen:

- The ViewModel exposes one `StateFlow<XUiState>`. State is immutable and uses `@StringRes` ids for messages.
- User intents are a sealed `XAction`, received by `onAction`.
- One-shot events are a sealed `XEffect` sent through `Channel<XEffect>(Channel.BUFFERED)`, so each is delivered once.
- `XRoute` owns the ViewModel, collects state with lifecycle awareness and collects effects with `ObserveEffects` (only while STARTED).
- `XScreen(state, onAction, ...)` is stateless. Every screen and shared component has a `@Preview`.
- Compose rules in detekt (`io.nlopez.compose.rules`) check the conventions: modifier parameters, state hoisting, stability.

## Consequences

- Screens render from plain data: previews and Compose UI tests need no Hilt.
- Effects are not lost while the app is in the background and not replayed after rotation.
- More files per screen (state, action, effect, route, screen).
- Text typed into fields can live in Compose state instead of the flow when a flow round-trip would drop characters (`DashboardViewModel.searchQuery`); this is an accepted exception.
