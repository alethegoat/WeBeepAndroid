<div align="center">
  <img src="docs/icon.png" width="200" alt="WeBeep App Icon">
  
  # WeBeep Android
  
  **A native Android client for Politecnico di Milano's WeBeep (Moodle) platform.**
  
  ![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue.svg?logo=kotlin)
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

- **Offline Support**: Courses, folders, and already-downloaded files stay on device for offline access.
- **Background Sync**: WorkManager periodically refreshes course contents (Wi-Fi-only by default; interval is in Settings). New files are indexed and notified, not auto-downloaded.
- **Notifications**: Alerts you when new files appear. Tapping the notification opens the course.
- **In-app viewers**: PDF (`PdfRenderer`), video/audio (Media3), images, and code, plus open-with for other types.
- **Search**: Filter courses on the dashboard and materials inside a course.
- **Polimi SSO**: Login via Chrome Custom Tabs using the official Politecnico authentication system. Tokens live in EncryptedSharedPreferences.
- **Bilingual Support**: Fully localized in English and Italian, including a per-app language setting.
- **Modern UI**: Jetpack Compose and Material 3, with dark mode and optional dynamic color.

---

### Build it yourself

1. Clone the repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Open the project in Android Studio.
3. Sync the project and run it on an emulator or device.

Release builds minify with R8 and fail fast if the release keystore is missing. If `KEYSTORE_FILE` is unset, set `ALLOW_DEBUG_RELEASE_SIGNING=true` for a debug-signed CI smoke build only. For Play, export:

```
KEYSTORE_FILE=/path/to/upload.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

Keep a backup of the upload keystore. Losing it means you cannot update the app on Play.

---

### Tech Stack

This project follows Clean Architecture and MVVM (StateFlow + Compose). Version `1.0.1` (`versionCode` 2), `minSdk` 26.

* **UI**: Jetpack Compose, Material 3
* **State**: Kotlin Coroutines & StateFlow
* **DI**: Dagger Hilt
* **Storage**: Room, Preferences DataStore, EncryptedSharedPreferences
* **Networking**: Ktor & OkHttp
* **Background**: WorkManager
* **Media**: Coil, Media3, Android `PdfRenderer`

---

### Security model

* **Tokens**: stored in EncryptedSharedPreferences (StrongBox when available), excluded from cloud/device backup. Never persisted in Room or navigation args (`stripAuthFromUrl`). Moodle requires tokens as URL query params: responses carrying them are `no-store`, and logs redact `wstoken`/`token`/`private_token`/Bearer.
* **WebView**: JavaScript on (course content needs it), but no file/content access, no popups, no autoplay; TLS errors fail closed; third-party cookies off.
* **Files**: app-private storage only, shared outward via FileProvider; logout wipes Room, downloads, caches, cookies and WebView data.
* **Builds**: release fails fast without a keystore; R8 minifies and strips verbose logs.

---

## 🇮🇹 Italiano

WeBeep Android è un'app nativa che semplifica l'accesso ai corsi di WeBeep per gli studenti del Politecnico di Milano. È sviluppata con un approccio **offline-first**, per permetterti di aprire i tuoi materiali di studio anche quando non hai una connessione internet.

### Funzionalità

- **Supporto Offline**: Corsi, cartelle e file già scaricati restano sul telefono senza rete.
- **Sync in Background**: WorkManager aggiorna periodicamente i contenuti dei corsi (di default solo Wi-Fi; frequenza nelle Impostazioni). I file nuovi vengono indicizzati e notificati, non scaricati in automatico.
- **Notifiche**: Avviso quando compaiono file nuovi. Il tap apre il corso.
- **Viewer in-app**: PDF (`PdfRenderer`), video/audio (Media3), immagini e codice; il resto si apre con un'app esterna.
- **Ricerca**: Filtra i corsi in dashboard e i materiali dentro un corso.
- **Login ufficiale**: SSO Politecnico via Chrome Custom Tabs. I token stanno in EncryptedSharedPreferences.
- **Multilingua**: Italiano e inglese, anche come lingua dell'app.
- **Interfaccia**: Jetpack Compose e Material 3, con Dark Mode e colori dinamici opzionali.

---

### Compilazione manuale

1. Clona la repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Apri il progetto in Android Studio.
3. Fai la sync di Gradle e avvia l'app su un emulatore o un telefono fisico.

Le build release usano R8 e falliscono se il keystore release manca. Per uno smoke test CI firmato debug usa `ALLOW_DEBUG_RELEASE_SIGNING=true`. Per Play:

```
KEYSTORE_FILE=/path/to/upload.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

Tieni un backup del keystore di upload. Senza quello non puoi aggiornare l'app sullo store.

---

### Tech Stack

Il progetto segue Clean Architecture e MVVM (StateFlow + Compose). Versione `1.0.1` (`versionCode` 2), `minSdk` 26.

* **UI**: Jetpack Compose, Material 3
* **State**: Kotlin Coroutines & StateFlow
* **DI**: Dagger Hilt
* **Storage**: Room, Preferences DataStore, EncryptedSharedPreferences
* **Rete**: Ktor & OkHttp
* **Background**: WorkManager
* **Media**: Coil, Media3, `PdfRenderer` di Android

---

### Modello di sicurezza

* **Token**: in EncryptedSharedPreferences (StrongBox dove disponibile), esclusi da backup cloud/dispositivo. Mai persistiti in Room o negli args di navigazione (`stripAuthFromUrl`). Moodle li richiede come query param: le risposte che li contengono sono `no-store` e i log oscurano `wstoken`/`token`/`private_token`/Bearer.
* **WebView**: JavaScript attivo (serve ai contenuti), ma niente accesso file/content, popup o autoplay; errori TLS bloccanti; niente cookie di terze parti.
* **File**: solo storage privato dell'app, condivisi via FileProvider; il logout cancella Room, download, cache, cookie e dati WebView.
* **Build**: la release fallisce senza keystore; R8 minifica e rimuove i log verbosi.

---

> **Disclaimer** <br>
> This is an unofficial application developed by students and is not affiliated with, maintained, or in any way endorsed by Politecnico di Milano. <br>
> *Questa è un'applicazione non ufficiale sviluppata da studenti e non è in alcun modo affiliata o approvata dal Politecnico di Milano.*
