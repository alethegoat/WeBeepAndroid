<div align="center">
  <img src="docs/icon.png" width="200" alt="WeBeep App Icon">
  
  # WeBeep Android
  
  **A native Android client for Politecnico di Milano's WeBeep (Moodle) platform.**
  
  ![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?logo=kotlin)
  ![Android](https://img.shields.io/badge/Android-8.0+-green.svg?logo=android)
  ![Jetpack Compose](https://img.shields.io/badge/Compose-Material_3-4285F4.svg?logo=android)
  ![License](https://img.shields.io/github/license/alethegoat/WeBeepAndroid)
</div>

---

<div align="center">
  <a href="#-italiano">🇮🇹 Italiano</a> | <a href="#-english">🇬🇧 English</a>
</div>

---

## 🇬🇧 English

WeBeep Android is a native application designed to make it easier for Politecnico di Milano students to access their courses on WeBeep. It's built with an **offline-first** approach, meaning you can open your study materials even when you don't have an internet connection.

### Features

- **Offline Support**: Courses, folders, and files are cached locally for offline access.
- **Background Sync**: Uses WorkManager to periodically check and download new materials uploaded by professors.
- **Notifications**: Alerts you when new files are available. Tapping the notification takes you directly to the course.
- **PDF Viewer**: Includes a custom PDF rendering engine optimized for smooth scrolling of large academic documents.
- **Polimi SSO**: Secure login via Chrome Custom Tabs using the official Politecnico authentication system.
- **Modern UI**: Built entirely with Jetpack Compose following Material 3 guidelines (with Dark Mode support).

---

### Download

You can download the latest compiled APK directly from the GitHub Releases page:

<div align="center">
    <a href="https://github.com/alethegoat/WeBeepAndroid/releases/latest">
        <img src="https://img.shields.io/badge/Download-APK-2ea44f?style=for-the-badge&logo=android" alt="Download APK" />
    </a>
</div>

#### Build it yourself

1. Clone the repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Open the project in Android Studio.
3. Sync the project and run it on an emulator or device.

---

### Tech Stack

This project follows Clean Architecture and the MVI (Model-View-Intent) pattern.

* **UI**: Jetpack Compose
* **State Management**: Kotlin Coroutines & StateFlow
* **DI**: Dagger Hilt
* **Storage**: Room & Preferences DataStore
* **Networking**: Ktor & OkHttp

---

## 🇮🇹 Italiano

WeBeep Android è un'app nativa che semplifica l'accesso ai corsi di WeBeep per gli studenti del Politecnico di Milano. È sviluppata con un approccio **offline-first**, per permetterti di aprire i tuoi materiali di studio anche quando non hai una connessione internet.

### Funzionalità

- **Supporto Offline**: Corsi, cartelle e file vengono salvati in locale per poterli consultare senza rete.
- **Sync in Background**: Controlla periodicamente se i professori hanno caricato nuovi materiali e li scarica in automatico.
- **Notifiche**: Ti avvisa quando ci sono nuovi file. Se premi la notifica, si apre direttamente la pagina del corso.
- **Lettore PDF**: Include un motore di rendering PDF ottimizzato per navigare velocemente anche i file più pesanti.
- **Login Ufficiale**: Accesso sicuro tramite il sistema SSO (Single Sign-On) ufficiale del Politecnico.
- **Interfaccia aggiornata**: Scritta interamente in Jetpack Compose, con pieno supporto al Material 3 e alla Dark Mode.

---

### Download

Puoi scaricare l'ultimo APK già compilato e pronto all'uso dalla pagina delle Release di GitHub:

<div align="center">
    <a href="https://github.com/alethegoat/WeBeepAndroid/releases/latest">
        <img src="https://img.shields.io/badge/Download-APK-2ea44f?style=for-the-badge&logo=android" alt="Download APK" />
    </a>
</div>

#### Compilazione manuale

1. Clona la repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Apri il progetto in Android Studio.
3. Fai la sync di Gradle e avvia l'app su un emulatore o un telefono fisico.

---

### Tech Stack

Il progetto segue le regole della Clean Architecture e usa il pattern MVI (Model-View-Intent).

* **UI**: Jetpack Compose
* **State Management**: Kotlin Coroutines & StateFlow
* **DI**: Dagger Hilt
* **Database**: Room & Preferences DataStore
* **Librerie di Rete**: Ktor & OkHttp

---

> **Disclaimer** <br>
> This is an unofficial application developed by students and is not affiliated with, maintained, or in any way endorsed by Politecnico di Milano. <br>
> *Questa è un'applicazione non ufficiale sviluppata da studenti e non è in alcun modo affiliata o approvata dal Politecnico di Milano.*
