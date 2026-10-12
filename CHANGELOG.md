# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

The app's `versionName` is still `1.0.1`. This section covers everything on `master` since the `v1.0.1` tag, ending with the code-quality stack (PRs #37 to #44).

### Added

- Background checks notify files that were first seen inside the app, and an unread notification accumulates new files instead of being replaced. Settings shows when the last check ran and has a "Check now" button.
- Persistent course folders, a richer file viewer and predictive back transitions.
- Adaptive layouts, and screens that recreate in place on rotation.
- Restyled settings (grouped, tonal surfaces) and a new "layered glass" launcher icon.
- Bundled Outfit and Inter fonts, Material 3 colour roles and spacing tokens.
- ktlint (blocking), detekt with Compose rules (blocking) and a Kover line-coverage floor of 75% in CI.
- Optional `scripts/pre-commit` hook, `docs/ARCHITECTURE.md`, architecture decision records in `docs/adr` and `CONTRIBUTING.md`.
- 488 JVM unit tests, including Room DAO and migration tests and Compose UI tests of every screen (Robolectric).

### Changed

- Every feature is layered as `presentation -> domain <- data`. Domain code is pure Kotlin. Each feature has its own Hilt module in `feature/<x>/di`.
- One error model: `Resource<T>` / `DataError` in `core/common`. The UI maps errors to localized strings.
- Every screen has one `StateFlow<XUiState>`, one-shot effects through a `Channel`, and a stateful `XRoute` / stateless `XScreen` split with previews.
- Navigation uses type-safe `@Serializable` routes, with one destination builder per feature.
- Credentials are stored in a Tink-encrypted store backed by Android Keystore instead of `EncryptedSharedPreferences`. Existing installs are migrated.
- New application id `io.github.alethegoat.webeep`. Target SDK 36, compile SDK 37, Kotlin 2.4.20, AGP 9.4.1.
- Course and dashboard lists load lazily per row. Detail screens slide so the back gesture tracks the finger.
- A download tracker is shared by every screen that offers a download.

### Fixed

- An expired token in the file viewer now says "session expired" instead of a generic error.
- Downloaded files with spaces in the name get the right MIME type.
- Coroutine cancellation is no longer reported as an error.
- The snackbar dismiss button now has an accessibility label.
- Background checks that were cancelled or lost notifications now resume and deliver them later.
- New and updated file notifications are reported correctly.
- Navigation jank in the course list.

### Security

- The user's full name is no longer stored. Neither is the SSO `private_token`.
- Tokens no longer travel in navigation routes. The SSO callback goes through an in-memory relay.
- URLs are stripped of tokens before they reach Room or a route.
- Tokens are sent in the POST body, and responses that carry them are `no-store`. Logs redact `wstoken`, `token`, `private_token` and Bearer values.
- Logout and a server-revoked session wipe the database, downloads, sync state, notifications, cookies and caches.
- Release builds fail without a keystore. The security audit of 2026-09 (`docs/AUDIT-2026-09.md`) was largely addressed.

### Removed

- Dead code and duplicated constants.

## [1.0.1] - 2026-06-06

### Changed

- Dynamic Moodle URLs and module filtering are handled in one place.
- Code smells fixed: hard-coded dispatchers, exception handling and regexes.
- README rewritten as a bilingual (EN/IT) document.

### Fixed

- Phantom `index.html` files returned by the Moodle API no longer cause false new-file notifications.

## [1.0.0] - 2026-06-06

First tagged version.

### Added

- Notifications grouped by course, with a deep link that opens the course.
- New launcher icon.

### Fixed

- Background sync and notification channel are initialized at startup.

[Unreleased]: https://github.com/alethegoat/WeBeepAndroid/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/alethegoat/WeBeepAndroid/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/alethegoat/WeBeepAndroid/releases/tag/v1.0.0
