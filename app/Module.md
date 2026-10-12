# Module WeBeep Android

Unofficial Android client for WeBeep, the Moodle site of Politecnico di Milano. It signs the student in through the
university SSO, caches their courses and files for offline use, downloads and previews materials, and notifies them
in the background when new files appear.

The app is a single Gradle module organised by layer and feature:

- `core` holds what several features share: errors and results, the Moodle HTTP client, Room, credential storage,
  session handling and the Compose design system.
- `feature/<name>` holds one feature each, split into `presentation`, `domain`, `data` and `di`.
- `navigation` wires the feature destinations into one graph.

Dependencies point one way: `presentation` depends on `domain`, and `data` implements `domain`, so `domain` depends
on neither. The `domain` packages are pure Kotlin and import no Android types. Features reach each other through
`domain` interfaces, and the Hilt modules in `di` are where an interface is bound to its implementation.

Security invariants that the whole codebase relies on: the session token is stored only by the encrypted
credential store; it travels in request bodies and is appended to file URLs only for the trusted WeBeep host;
URLs are passed through `stripAuthFromUrl` before they reach Room, navigation arguments or UI state.

# Package it.polimi.webeep

Application entry points: the `Application` class (Hilt root, WorkManager and image-loader setup), the single
`MainActivity` and the `WeBeepApp` composable that hosts theme, session gating and navigation.

# Package it.polimi.webeep.core.common

Pure Kotlin building blocks shared by every layer: `Resource` and `DataError` for typed failures, offline-first
loading (`cacheThenRefresh`), Moodle text and URL helpers, file naming utilities and the `ConnectivityObserver`
contract. It depends on nothing else in the app.

# Package it.polimi.webeep.core.network

The Moodle REST client and the OkHttp interceptors around it (token injection, retry, cache hygiene, log
redaction), plus connectivity monitoring. Tokens are added here and only for the trusted host; the layer above
never sees them in a URL.

# Package it.polimi.webeep.core.network.dto

Serializable mirrors of the Moodle web-service responses. They are mapped to domain models and Room entities in
the data layer and never leave it.

# Package it.polimi.webeep.core.database

The Room database and its migrations: a rebuildable offline cache of courses and their contents. It never stores
credentials, and schema changes need a migration plus an exported schema.

# Package it.polimi.webeep.core.database.dao

Room DAOs for the course list and for course contents, including the transactional replace operations that keep
the UI from seeing half-written data.

# Package it.polimi.webeep.core.database.entity

Room entities, one table each. URLs in them are stored without tokens.

# Package it.polimi.webeep.core.datastore

Encrypted credential storage (Tink AEAD under an Android Keystore key) behind `TokenProvider`. Its `suspend`
members are for coroutines; the `*Sync` members exist for OkHttp interceptors that cannot suspend.

# Package it.polimi.webeep.core.session

The contracts for ending a session: `SessionDataCleaner` erases all local user data and `SessionExpiryHandler`
turns a lost session into that wipe. The implementation lives in `core.session.data`.

# Package it.polimi.webeep.core.session.data

`SessionDataCleaner` implementation that wipes the database, downloads, sync state, notifications, web session and
caches, step by step so one failure cannot leave data behind.

# Package it.polimi.webeep.core.storage

`LocalFileStore`, a size-bounded directory of app-private files with atomic replacement, shared by downloads and
the viewer cache, and MIME type lookup by file name.

# Package it.polimi.webeep.core.di

Hilt modules for what core provides: dispatchers and their qualifiers, the HTTP stack, the database, the
credential store and the core bindings. Feature bindings live in each feature's own `di` package.

# Package it.polimi.webeep.core.ui.components

Reusable Compose components of the design system: backgrounds, top bars, empty and loading states, snackbars,
shimmer placeholders and the search field. Presentation only; they know nothing about features.

# Package it.polimi.webeep.core.ui.theme

Theme, color, type, spacing and motion tokens of the WeBeep design system.

# Package it.polimi.webeep.core.ui.util

Compose helpers: one-shot effect collection, press and entrance animations, zoom state, window size classes,
reduced-motion support and external-app intents.

# Package it.polimi.webeep.feature.auth.domain

Login rules, free of Android types: the SSO passport handshake, callback parsing and the relay that carries the
callback from the activity to the login screen. `AuthRepository` is the contract that `data` implements.

# Package it.polimi.webeep.feature.auth.data

`AuthRepository` implementation over Moodle and the credential store. Logout cancels background sync and wipes
local data in a fixed order.

# Package it.polimi.webeep.feature.auth.di

Hilt binding of the auth repository.

# Package it.polimi.webeep.feature.auth.presentation

Login screen, its ViewModel and the Custom Tab launcher for SSO. Depends on `domain` only.

# Package it.polimi.webeep.feature.auth.presentation.components

Composable parts of the login screen.

# Package it.polimi.webeep.feature.dashboard.domain

Course-list use cases (cache first, then network) and the `DashboardRepository` contract.

# Package it.polimi.webeep.feature.dashboard.domain.model

Domain models of the dashboard: `Course` and `UserProfile`.

# Package it.polimi.webeep.feature.dashboard.data

`DashboardRepository` implementation over Room and Moodle; syncing replaces the cached course list in one
transaction.

# Package it.polimi.webeep.feature.dashboard.di

Hilt binding of the dashboard repository.

# Package it.polimi.webeep.feature.dashboard.presentation

Dashboard screen and ViewModel with search and filters kept in the saved state. Depends on `domain` only.

# Package it.polimi.webeep.feature.dashboard.presentation.components

Composable parts of the dashboard: course cards, grid, filters and skeletons.

# Package it.polimi.webeep.feature.course.domain

Use case that loads a course's contents (cache first, then network) and the `CourseRepository` contract.

# Package it.polimi.webeep.feature.course.domain.model

Domain models of a course's content tree: `Section`, `Module`, `FileResource` and the supported `ModuleType`s.

# Package it.polimi.webeep.feature.course.data

`CourseRepository` implementation and the single mapper from Moodle DTOs to domain models and Room entities, used
by both the interactive refresh and the background sync. Persisted URLs are token-free.

# Package it.polimi.webeep.feature.course.di

Hilt binding of the course repository.

# Package it.polimi.webeep.feature.course.presentation

Course detail screen and ViewModel: searchable sections, folders and files with per-file download state. Depends
on `domain` only.

# Package it.polimi.webeep.feature.course.presentation.components

Composable parts of the course screen: header, sections, folders, module and file rows.

# Package it.polimi.webeep.feature.download.domain

`DownloadRepository`, the use case that starts a download and `DownloadTracker`, which turns progress into
`DownloadState`s so every screen maps them the same way.

# Package it.polimi.webeep.feature.download.domain.model

`DownloadState`, shared by the course list and the file viewer.

# Package it.polimi.webeep.feature.download.data

OkHttp-based downloader writing atomically into app-private storage, with cancellation support and the one-shot
migration of downloads from older versions.

# Package it.polimi.webeep.feature.download.di

Hilt binding of the download repository.

# Package it.polimi.webeep.feature.fileviewer.domain

What the viewer needs to know, free of UI: `ViewerFile`, the classified `LoadedContent`, the lookup of sibling
files to swipe through and the `FileContentRepository` contract.

# Package it.polimi.webeep.feature.fileviewer.data

`FileContentRepository` implementation: downloads into a bounded cache, reuses saved downloads, classifies the
content and verifies PDFs before they reach the renderer.

# Package it.polimi.webeep.feature.fileviewer.di

Hilt binding of the file content repository.

# Package it.polimi.webeep.feature.fileviewer.presentation

File viewer screen and ViewModel: a pager over sibling files that loads only the visible page and its neighbours.
Depends on `domain` only.

# Package it.polimi.webeep.feature.fileviewer.presentation.components

Composable parts of the viewer: top bar, page host, image, media and code viewers.

# Package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

PDF rendering on top of `PdfRenderer`: page cache, session lifecycle, paging, zoom and the page indicator.

# Package it.polimi.webeep.feature.settings.domain

Settings contracts and the use cases that persist a sync option and then reschedule background sync.

# Package it.polimi.webeep.feature.settings.domain.model

Persisted setting values, `ThemePreference` and `SyncFrequency`. They are stored by name, so constants must not be
renamed.

# Package it.polimi.webeep.feature.settings.data

DataStore-backed `SettingsRepository`. The preference keys are the on-disk format and must not change.

# Package it.polimi.webeep.feature.settings.di

Hilt binding of the settings repository and the DataStore it uses.

# Package it.polimi.webeep.feature.settings.presentation

Settings screen and ViewModel: appearance, sync options, manual check and logout. Depends on `domain` only.

# Package it.polimi.webeep.feature.settings.presentation.components

Composable parts of the settings screen.

# Package it.polimi.webeep.feature.sync

`CourseSyncWorker`, the WorkManager job that refreshes courses in the background. Its class name is stored in
WorkManager's queue, so it must not be moved or renamed.

# Package it.polimi.webeep.feature.sync.domain

Pure sync rules: diffing course snapshots into added and updated files, merging pending changes, ordering courses,
and the contracts for scheduling, notifying and storing sync state.

# Package it.polimi.webeep.feature.sync.domain.model

Serializable sync state and change models. Their field names are the on-disk format of existing installs.

# Package it.polimi.webeep.feature.sync.data

WorkManager scheduling, the notifications and the atomic JSON store of the sync state.

# Package it.polimi.webeep.feature.sync.di

Hilt bindings of the sync contracts to their implementations.

# Package it.polimi.webeep.navigation

The navigation graph, type-safe routes, screen transitions and the helper that drops callbacks arriving while a
screen is not resumed. Each feature contributes its own destination; this package only connects them.
