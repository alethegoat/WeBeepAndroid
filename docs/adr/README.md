# Architecture decision records

Short records of decisions that shape the code, in [Nygard format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions). The overview is in [ARCHITECTURE.md](../ARCHITECTURE.md).

| # | Title | Status |
|---|---|---|
| [0001](0001-single-module-package-layering.md) | Single Gradle module with package-level feature layering | Accepted |
| [0002](0002-resource-dataerror.md) | `Resource`/`DataError` as the only data-layer result type | Accepted |
| [0003](0003-uistate-effects-route-screen.md) | Single UiState, Channel effects and Route/Screen split | Accepted |
| [0004](0004-type-safe-navigation.md) | Type-safe `@Serializable` navigation routes, no tokens in routes | Accepted |
| [0005](0005-encrypted-secure-store.md) | Tokens in an encrypted Tink `SecureStore` | Accepted |
| [0006](0006-jvm-first-testing.md) | JVM-first testing with Robolectric and a Kover ratchet | Accepted |

To add one: copy the shape of an existing file, use the next number, and add a row here. Do not rewrite an accepted record; add a new one that supersedes it.
