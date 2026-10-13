# 0004. Type-safe `@Serializable` navigation routes, no tokens in routes

Status: Accepted

## Context

String routes with manual argument encoding were fragile. Route arguments are saved with the back stack and appear in logs and intents, so anything placed there is persisted. File URLs and the SSO callback blob can contain access tokens.

## Decision

- Destinations are `@Serializable` types in `navigation/Routes.kt` (`Login`, `Dashboard`, `Settings`, `CourseDetail`, `FileViewer`) used with Navigation Compose's type-safe API.
- Each feature exposes one `NavGraphBuilder` extension for its destination. `NavGraph.kt` connects them with callbacks.
- Routes carry only what a screen cannot look up. `CourseDetail` has an id. `FileViewer` has a URL, and that URL has passed `stripAuthFromUrl`.
- The SSO callback blob goes from `MainActivity` to the login screen through the in-memory `SsoCallbackRelay`, not a route, saved state or intent.

## Consequences

- Argument types are checked at compile time; ViewModels read them with `SavedStateHandle.toRoute<T>()`.
- Adding data to a route means checking it for secrets first.
- Features do not import each other's screens; only `navigation` does.
- File requests re-attach the token at the network layer (`PluginFileAuthInterceptor`), so a stripped URL still loads.
