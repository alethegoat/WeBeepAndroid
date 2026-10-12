# 0006. JVM-first testing with Robolectric and a Kover ratchet

Status: Accepted

## Context

Instrumented tests need an emulator, are slow and are hard to run in CI. Room, migrations and Compose screens are the code that most needs them. Coverage could drop silently.

## Decision

- Tests run on the JVM with `./gradlew test`. Room DAO tests use Robolectric with an in-memory database; migration tests use `MigrationTestHelper` with the exported schemas in `app/schemas`. Compose UI tests use Robolectric and `createComposeRule` on stateless screens, with semantic finders. `app/src/test/resources/robolectric.properties` pins `sdk=34`.
- Fakes are hand-written classes in `app/src/test/java/it/polimi/webeep/util/fakes`, not mocks, for domain interfaces. ViewModel tests use `MainDispatcherRule` and Turbine.
- `TokenManager` stays in `app/src/androidTest` because Robolectric has no Android Keystore. The pure parts (`AuthRecord`, `PendingPassports`, `SecureValueCodec`) are covered on the JVM.
- Kover reports the debug variant and `koverVerifyDebug` fails below 75% line coverage. Generated Hilt/Room code, `BuildConfig`, `R` and `@Preview` functions are excluded. The bound is a ratchet: set to the measured value rounded down, raised when coverage rises.

## Consequences

- The full suite (488 unit tests) runs without a device, in CI on JDK 17.
- Robolectric is not a real device: animations, the Keystore, WorkManager's real scheduling and PdfRenderer need manual checks.
- Compose tests depend on screens being stateless (ADR 0003).
- Lowering the Kover bound needs a reason in the PR.
