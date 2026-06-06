<p align="center">
  <img src="docs/icon.png" width="150" alt="WeBeep App Icon">
</p>

# WeBeep Android

A native Android client for Politecnico di Milano's WeBeep (Moodle) platform, designed with an offline-first architecture to improve access to university resources.

## Overview

This project provides a mobile interface for students to access their WeBeep courses, view materials, and receive updates. It focuses on offline availability, security, and performance, specifically regarding the rendering of large academic documents.

## Key Features

- **Offline Support**: Courses, sections, and downloaded files are cached locally using Room, allowing access without an active internet connection.
- **SSO Authentication**: Integration with Politecnico di Milano's Single Sign-On via Chrome Custom Tabs.
- **Custom PDF Engine**: Optimized PDF rendering pipeline using asynchronous pre-fetching and LRU Bitmap pooling to ensure smooth scrolling through heavy documents.
- **Background Synchronization & Notifications**: WorkManager periodic tasks handle background diffing, pushing system notifications when new materials are uploaded by professors.
- **Smart Deep Linking**: Tapping on a new material notification seamlessly routes the user directly to the specific course detail screen.
- **Modern & Premium UI**: Implemented entirely with Jetpack Compose following Material 3 guidelines, featuring a custom high-end launcher icon, glassmorphism elements, and dark mode support.

## Architecture & Tech Stack

The application follows the principles of Clean Architecture and MVI (Model-View-Intent).

- **UI**: Jetpack Compose
- **State Management**: Kotlin Coroutines & StateFlow
- **Dependency Injection**: Dagger Hilt
- **Local Storage**: Room Database & Preferences DataStore
- **Networking**: Retrofit2 & OkHttp
- **Background Processing**: WorkManager

## Setup and Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Open the project in Android Studio (Koala or newer recommended).
3. Ensure JDK 17+ is installed.
4. Sync Gradle and run the application.

## Project Structure

- `core`: Shared UI components, theme definitions, database DAOs, network DTOs, and DI setup.
- `feature/auth`: Authentication flow and secure token management.
- `feature/dashboard`: Course overview, filtering, and manual synchronization logic.
- `feature/course`: Course content parsing and module traversal.
- `feature/fileviewer`: PDF rendering engine and media viewer.
- `feature/sync`: Background worker and notification dispatcher.

## Disclaimer

This is an unofficial client and is not affiliated with, maintained, or endorsed by Politecnico di Milano.
