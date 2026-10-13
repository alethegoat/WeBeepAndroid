<div align="center">
  <img src="docs/icon.png" width="200" alt="WeBeep App Icon">
  
  # WeBeep Android
  
  **A native Android client for Politecnico di Milano's WeBeep (Moodle) platform.**
  
  ![Kotlin](https://img.shields.io/badge/Kotlin-2.4-blue.svg?logo=kotlin)
  ![Android](https://img.shields.io/badge/Android-8.0+-green.svg?logo=android)
  ![Jetpack Compose](https://img.shields.io/badge/Compose-Material_3-4285F4.svg?logo=android)
  ![License](https://img.shields.io/github/license/alethegoat/WeBeepAndroid)
</div>

---

<div align="center">
  <a href="#-italiano">🇮🇹 Italiano</a> | <a href="#-english">🇬🇧 English</a> | <a href="PRIVACY.md">🔒 Privacy</a>
</div>

---

## 🇬🇧 English

WeBeep Android is a native application designed to make it easier for Politecnico di Milano students to access their courses on WeBeep. It's built with an **offline-first** approach, meaning you can open your study materials even when you don't have an internet connection.

### Features

- **Offline Support**: Courses, folders, and already-downloaded files stay on device for offline access.
- **Background Sync**: WorkManager periodically refreshes course contents (Wi-Fi-only by default; interval is in Settings). New files are indexed and notified, not auto-downloaded.
- **Notifications**: Alerts you only about files that were added or updated since the last sync (one notification per course, grouped). Files are notified even if you already opened the course in the app, and an unread notification accumulates new files instead of being replaced. Settings shows when the last check ran and has a "Check now" button. Tapping the notification opens the course.
- **In-app viewers**: PDF (`PdfRenderer`), video/audio (Media3), images, and code, plus open-with for other types.
- **Search**: Filter courses on the dashboard and materials inside a course.
- **Polimi SSO**: Login via Chrome Custom Tabs using the official Politecnico authentication system. The access token is stored encrypted (Tink AES-256-GCM, key wrapped by Android Keystore).
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

Release builds minify with R8 and fail fast if the release keystore is missing. This applies to every task that packages or signs the release (`assemble`, `build`, `bundle` included). If `KEYSTORE_FILE` is unset, set `ALLOW_DEBUG_RELEASE_SIGNING=true` for a debug-signed CI smoke build only. For Play, export:

```
KEYSTORE_FILE=/path/to/upload.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

Keep a backup of the upload keystore. Losing it means you cannot update the app on Play.

---

### Tech Stack

This project follows Clean Architecture and MVVM (StateFlow + Compose). Version `1.0.1` (`versionCode` 2), `minSdk` 26. Layers, data flow and conventions are described in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); how to contribute is in [CONTRIBUTING.md](CONTRIBUTING.md). CI enforces ktlint, detekt (with Compose rules) and a Kover line-coverage floor.

* **UI**: Jetpack Compose, Material 3; Outfit + Inter fonts bundled (SIL OFL 1.1, texts in `app/src/main/assets/licenses`)
* **State**: Kotlin Coroutines & StateFlow
* **DI**: Dagger Hilt
* **Storage**: Room, Preferences DataStore, Tink-encrypted secure store (Android Keystore)
* **Networking**: Ktor & OkHttp
* **Background**: WorkManager
* **Media**: Coil, Media3, Android `PdfRenderer`

---

### Security model

* **Tokens**: the `wstoken` is stored in a Tink AES-256-GCM `SecureStore` whose keyset is wrapped by an Android Keystore key, in a file excluded from cloud backup and device transfer. If Keystore is unusable, nothing is written in clear. The SSO `private_token` is discarded and your full name is not stored. Tokens never reach Room or navigation args (`stripAuthFromUrl`). API calls send the token in the POST body; Moodle requires it as a URL query param for file downloads, and responses carrying them are `no-store`, and logs redact `wstoken`/`token`/`private_token`/Bearer.
* **No WebView**: in-app viewers are native (PDF, Media3, images, code); web pages open in Custom Tabs.
* **Files**: downloads live in app-private internal storage (`files/downloads`) and are shared outward only via FileProvider.
* **Session**: logout, and a forced logout when the server revokes the token, wipe Room, downloads, caches and cookies, then return to the login screen.
* **Builds**: release packaging/signing fails fast without a keystore; R8 minifies and strips verbose logs. CI runs lint, detekt, ktlint, Kover and validates the Gradle wrapper; Dependabot tracks dependencies.

See [PRIVACY.md](PRIVACY.md) for the data the app handles.

---

## 🇮🇹 Italiano

WeBeep Android è un'app nativa che semplifica l'accesso ai corsi di WeBeep per gli studenti del Politecnico di Milano. È sviluppata con un approccio **offline-first**, per permetterti di aprire i tuoi materiali di studio anche quando non hai una connessione internet.

### Funzionalità

- **Supporto Offline**: Corsi, cartelle e file già scaricati restano sul telefono senza rete.
- **Sync in Background**: WorkManager aggiorna periodicamente i contenuti dei corsi (di default solo Wi-Fi; frequenza nelle Impostazioni). I file nuovi vengono indicizzati e notificati, non scaricati in automatico.
- **Notifiche**: Avviso solo per i file aggiunti o aggiornati dall'ultima sincronizzazione (una notifica per corso, raggruppate). I file vengono notificati anche se hai già aperto il corso nell'app, e una notifica non ancora letta accumula i nuovi file invece di essere sostituita. Nelle Impostazioni si vede quando è stato fatto l'ultimo controllo e c'è il pulsante "Controlla ora". Il tap apre il corso.
- **Viewer in-app**: PDF (`PdfRenderer`), video/audio (Media3), immagini e codice; il resto si apre con un'app esterna.
- **Ricerca**: Filtra i corsi in dashboard e i materiali dentro un corso.
- **Login ufficiale**: SSO Politecnico via Chrome Custom Tabs. Il token di accesso è salvato cifrato (Tink AES-256-GCM, chiave protetta da Android Keystore).
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

Le build release usano R8 e falliscono se il keystore release manca, con qualunque task che impacchetta o firma la release (anche `assemble`, `build`, `bundle`). Per uno smoke test CI firmato debug usa `ALLOW_DEBUG_RELEASE_SIGNING=true`. Per Play:

```
KEYSTORE_FILE=/path/to/upload.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

Tieni un backup del keystore di upload. Senza quello non puoi aggiornare l'app sullo store.

---

### Tech Stack

Il progetto segue Clean Architecture e MVVM (StateFlow + Compose). Versione `1.0.1` (`versionCode` 2), `minSdk` 26. Livelli, flusso dei dati e convenzioni sono in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); come contribuire è in [CONTRIBUTING.md](CONTRIBUTING.md). La CI impone ktlint, detekt (con regole Compose) e una soglia minima di copertura Kover.

* **UI**: Jetpack Compose, Material 3; font Outfit + Inter inclusi (SIL OFL 1.1, testi in `app/src/main/assets/licenses`)
* **State**: Kotlin Coroutines & StateFlow
* **DI**: Dagger Hilt
* **Storage**: Room, Preferences DataStore, Tink-encrypted secure store (Android Keystore)
* **Rete**: Ktor & OkHttp
* **Background**: WorkManager
* **Media**: Coil, Media3, `PdfRenderer` di Android

---

### Modello di sicurezza

* **Token**: il `wstoken` sta in uno `SecureStore` cifrato con Tink AES-256-GCM, il cui keyset è protetto da una chiave Android Keystore, in un file escluso da backup cloud e trasferimento tra dispositivi. Se il Keystore non è utilizzabile non viene scritto nulla in chiaro. Il `private_token` dell'SSO viene scartato e il tuo nome completo non viene salvato. I token non finiscono mai in Room o negli args di navigazione (`stripAuthFromUrl`). Le chiamate API inviano il token nel body POST; Moodle lo richiede come query param per il download dei file, e le risposte che lo contengono sono `no-store` e i log oscurano `wstoken`/`token`/`private_token`/Bearer.
* **Niente WebView**: i viewer in-app sono nativi (PDF, Media3, immagini, codice); le pagine web si aprono in Custom Tabs.
* **File**: i download stanno nello storage interno privato dell'app (`files/downloads`) e sono condivisi solo via FileProvider.
* **Sessione**: il logout, e il logout forzato quando il server revoca il token, cancellano Room, download, cache e cookie, poi riportano al login.
* **Build**: packaging e firma release falliscono senza keystore; R8 minifica e rimuove i log verbosi. La CI esegue lint, detekt, ktlint, Kover e valida il Gradle wrapper; Dependabot tiene d'occhio le dipendenze.

Vedi [PRIVACY.md](PRIVACY.md) per i dati trattati dall'app.

---

> **Disclaimer** <br>
> This is an unofficial application developed by students and is not affiliated with, maintained, or in any way endorsed by Politecnico di Milano. <br>
> *Questa è un'applicazione non ufficiale sviluppata da studenti e non è in alcun modo affiliata o approvata dal Politecnico di Milano.*
