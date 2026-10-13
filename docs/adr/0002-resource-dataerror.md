# 0002. `Resource`/`DataError` as the only data-layer result type

Status: Accepted

## Context

Failures can be modelled as raw exceptions, `Result`, `runCatching` or ad-hoc sealed types, and mixing them is hard to map to the UI. Technical exception messages could reach the UI. `runCatching` around suspending calls swallowed `CancellationException`, so cancelled work was reported as an error and structured concurrency broke.

## Decision

- Data-layer calls return `Resource<T>`: `Success(data)` or `Error(DataError, throwable)` (`core/common/Resource.kt`).
- `DataError` is a closed set: `Network`, `Unauthorized`, `Server(errorCode)`, `Storage`, `Unknown`. `Throwable.toDataError()` is the single mapping.
- `resourceOf { }` builds a `Resource` and rethrows `CancellationException`.
- Best-effort local code that does not return a `Resource` uses `runCatchingCancellable`, which has the same rule.
- The UI maps `DataError` to string resources (`core/ui/util/DataErrorMessages.kt`). Exception text is never displayed.
- Login keeps its own `LoginError` because it has more cases; its validation failure wraps a `DataError`.

## Consequences

- Cancelling a screen no longer shows an error.
- Every error shown to the user is localized and testable.
- Callers that need an exception again use `getOrThrow()` (the sync worker does).
- New failure kinds mean a new `DataError` case, which forces `when` expressions to be updated.
- Plain `runCatching` around suspend code is a bug in this codebase.
