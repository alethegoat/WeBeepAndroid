# Contributing

Thanks for helping. Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) first; it explains where code goes. Decisions are in [docs/adr](docs/adr/README.md).

## Prerequisites

- Android Studio (current stable) with the Android SDK. `compileSdk` is 37, `minSdk` is 26.
- Run Gradle with Android Studio's bundled JDK 21 (JBR). The default JDK 25 breaks detekt:

  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ```

- CI uses JDK 17 (Temurin). Do not rely on newer language or API levels of Java.

## The local gate

Run this before every push. CI runs the same checks (`.github/workflows/ci.yml`) and all of them block:

```bash
./gradlew :app:ktlintCheck :app:detekt :app:lintDebug test :app:koverVerifyDebug :app:assembleDebug
```

- ktlint: formatting. Fix it automatically with `./gradlew :app:ktlintFormat`.
- detekt: static analysis, including Compose rules. Config in `config/detekt/detekt.yml`; `config/detekt/baseline.xml` holds accepted legacy findings. Do not add to the baseline to silence new code.
- Android lint: `lintDebug`.
- `test`: JVM unit tests, including Robolectric.
- Kover: `koverVerifyDebug` fails below 75% line coverage. Raise the bound in `app/build.gradle.kts` when coverage rises; never lower it without saying why in the PR.

### Optional pre-commit hook

`scripts/pre-commit` runs `:app:ktlintCheck :app:detekt` when staged files include `.kt` or `.kts`. It prints a hint to run `ktlintFormat` on failure. Install it from the repo root:

```bash
ln -s ../../scripts/pre-commit .git/hooks/pre-commit
```

The hook calls `./gradlew`, so set `JAVA_HOME` as above in the shell you commit from.

## Code style

- ktlint with the `android_studio` code style (`.editorconfig`). Max line length 140, 4 spaces, LF.
- No trailing commas, in declarations or at call sites.
- Comments and KDoc are in English.
- KDoc explains why, not what. Do not restate the name or the signature. Document invariants, units, threading and non-obvious choices. detekt requires KDoc on public classes and functions in `domain`, `data` and `core/{common,network,datastore,session,storage}` (not on overrides); make it say something.
- User-visible text goes in string resources. Italian is the default locale (`res/values`); English is in `res/values-en`.
- Use the injected dispatchers (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`), not `Dispatchers.*`.

## Architecture rules

Summary; the full version is in [ARCHITECTURE.md](docs/ARCHITECTURE.md).

- `presentation -> domain <- data`. `domain` is pure Kotlin. Bindings go in `feature/<x>/di`.
- Failures are `Resource<T>` / `DataError`. Use `resourceOf` or `runCatchingCancellable`, never bare `runCatching` around suspending code.
- One `StateFlow<XUiState>` per ViewModel, one-shot effects through `Channel<XEffect>` and `ObserveEffects`, an `XRoute` that owns the ViewModel and a stateless `XScreen(state, onAction)` with a preview.
- Navigation uses `@Serializable` routes. Never put a token in a route, a log line or Room. Run URLs through `stripAuthFromUrl` before storing them.
- Do not rename or move `CourseSyncWorker`, the DataStore file and keys, `SyncState` fields, or the persisted enums `ThemePreference` and `SyncFrequency`. Installed apps depend on them.

## Tests

See [ADR 0006](docs/adr/0006-jvm-first-testing.md) for the reasoning.

- Sources: `app/src/test/java/it/polimi/webeep`, mirroring the main package tree. Instrumented tests: `app/src/androidTest` (only `TokenManager`, which needs the Keystore).
- Test names are backtick sentences: ``fun `expired token maps to session expired`()``.
- Use hand-written fakes from `app/src/test/java/it/polimi/webeep/util/fakes` (add new ones there) rather than mocks for domain interfaces.
- Coroutine tests use `MainDispatcherRule` (`util/MainDispatcherRule.kt`). Flows are asserted with Turbine.
- Robolectric tests use `@RunWith(RobolectricTestRunner::class)`. `app/src/test/resources/robolectric.properties` pins the SDK to 34.
- Compose UI tests target the stateless `XScreen` and use semantic finders (`onNodeWithText`, `onNodeWithContentDescription`). Use `setAppContent` and `string(...)` from `util/ComposeTestSupport.kt`. Use a test tag only when no semantic finder fits.
- Fix a bug with a test that fails first.

## Branches and pull requests

- Branch from `master`. Use a prefix and a short name: `feature/<name>`, `fix/<name>`, `quality/<n>-<name>`, `chore/<name>`, `docs/<name>`.
- Keep PRs small and about one thing. Split refactors from behavior changes.
- CI must be green. Run the gate locally first.
- Add a line to `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md) for anything a user or contributor would notice.
- Update the docs you touched. Architecture changes that reverse a decision need a new ADR.
- Commit messages: a short imperative summary line, then a body explaining why when it is not obvious.

## Release signing

Release builds need a keystore and fail without one. Set `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`, as described in the README, [Build it yourself](README.md#build-it-yourself). `ALLOW_DEBUG_RELEASE_SIGNING=true` is for CI smoke builds only. Never commit keystores or passwords.

## Releases

1. Bump `versionName` in `app/build.gradle.kts` and move the `## [Unreleased]` entries of [CHANGELOG.md](CHANGELOG.md) under a `## [x.y.z]` heading.
2. Push a tag `vX.Y.Z` that matches `versionName`. The Release workflow builds the signed APK and AAB and publishes them with the changelog section as notes. To rebuild an existing tag, run the workflow manually with that tag.
3. The repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` must be set. The workflow fails if any is empty or if the APK is debug-signed.
