# WeBeep Android 🚀

An unofficial, blazingly fast, and meticulously engineered native Android client for WeBeep (Politecnico di Milano's Moodle platform). Built to deliver a premium user experience with offline-first capabilities, 60fps PDF rendering, and background synchronization.

## ✨ Features

* **Offline-First Architecture**: View your courses, sections, and downloaded materials even without an internet connection. Powered by an atomic transactional Room database.
* **Premium UX & Material 3**: A modern, immersive UI built entirely with Jetpack Compose. Features glassmorphism, fluid micro-animations, and full dark-mode support.
* **Polimi SSO Login**: Secure and seamless authentication using Chrome Custom Tabs for Politecnico di Milano's Single Sign-On.
* **Next-Gen PDF Viewer**: Read complex academic slides with zero lag. Features asynchronous background pre-fetching, LRU Bitmap caching, and custom pinch-to-zoom multi-touch handling for a flawless 60fps scrolling experience.
* **Background Sync & Push Notifications**: Never miss a new lecture. A smart WorkManager periodically diffs your local cache against Moodle APIs and intelligently pushes grouped system notifications when a professor uploads new materials.

## 🛠️ Tech Stack

This project strictly adheres to Modern Android Development (MAD) guidelines and Senior/Staff engineering standards.

* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Architecture**: Clean Architecture & MVI (Model-View-Intent)
* **Concurrency**: Kotlin Coroutines & `StateFlow`
* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) (via KSP)
* **Database**: [Room](https://developer.android.com/training/data-storage/room)
* **Networking**: [Retrofit2](https://square.github.io/retrofit/) & OkHttp (with custom Token/Retry Interceptors)
* **Background Work**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
* **Media Loading**: [Coil](https://coil-kt.github.io/coil/)

## 🚀 Getting Started

### Prerequisites
* Android Studio (Koala or newer recommended)
* JDK 17+
* Minimum SDK: Android 8.0 (API 26)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/WeBeepAndroid.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on an emulator or physical device.

## 🏗️ Codebase Structure

The app is divided into cohesive feature modules:

* `core`: Shared UI components, theme definitions, database entities/DAOs, network DTOs, and DI configurations.
* `feature/auth`: SSO login flow, intent interceptors, and secure token storage.
* `feature/dashboard`: Course grid, pull-to-refresh logic, filtering, and offline sync state management.
* `feature/course`: Detailed view of a specific course, module tree traversal, and file downloading.
* `feature/fileviewer`: The custom PDF rendering engine and media previewer.
* `feature/sync`: Background `CourseSyncWorker` and smart notification builder.

> **Note:** All internal documentation, KDocs, and inline comments are strictly standardized in lowercase English to ensure consistency and readability.

## 📝 License

This project is not officially affiliated with Politecnico di Milano. It was built as a personal initiative to improve the mobile student experience.
