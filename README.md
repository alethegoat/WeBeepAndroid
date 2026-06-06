<div align="center">
  <img src="docs/icon.png" width="200" alt="WeBeep App Icon">
  
  # WeBeep Android
  
  **The native Android client designed for Politecnico di Milano students.**
  
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

### What is it exactly?

WeBeep Android is a native application designed to radically improve the mobile experience of Politecnico di Milano students with the WeBeep (Moodle) platform. The app is built with an **Offline-First** architecture, allowing you to study, consult documents, and receive updates wherever you are, even on the subway or without a connection.

### 🚀 Key Features

- 📱 **Premium & Modern UI**: A meticulously crafted interface, developed entirely in *Jetpack Compose* following Material Design 3 standards (including a beautiful Dark Mode and a Glassmorphism icon).
- ☁️ **Background Sync**: Never miss a file. The app uses `WorkManager` to silently download new materials uploaded by professors.
- 🔔 **Notifications & Smart Deep Linking**: When a new document is released, you receive a push notification. Tapping it will automatically teleport you straight into the specific course, right to the new material.
- ⚡ **Custom PDF Engine**: Advanced PDF handling with asynchronous rendering and LRU Bitmap caching. No more freezes or slowdowns in gigantic academic files!
- 🔐 **Secure SSO Authentication**: Seamlessly integrated login with official PoliMi services via Chrome Custom Tabs.

---

### 📥 Download and Installation

The project is currently under active development. You can download the latest **APK** directly from GitHub Releases:

<div align="center">
    <a href="https://github.com/alethegoat/WeBeepAndroid/releases/latest">
        <img src="https://img.shields.io/badge/Download_APK-Latest_Release-2ea44f?style=for-the-badge&logo=android" alt="Download APK" />
    </a>
</div>

#### Manual Build

1. Clone the repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Open the project in **Android Studio** (latest stable version recommended).
3. Ensure JDK 17+ is installed.
4. Sync Gradle and hit *Run*.

---

### 🛠 Tech Stack and Architecture

The application follows **Clean Architecture** principles and adopts the **MVI** (Model-View-Intent) pattern.

* **User Interface**: Jetpack Compose
* **State Management & Concurrency**: Kotlin Coroutines & StateFlow
* **Dependency Injection**: Dagger Hilt
* **Database & Cache**: Room Database & Preferences DataStore
* **Networking**: Retrofit2 & OkHttp

---

## 🇮🇹 Italiano

### Cos'è esattamente?

WeBeep Android è un'applicazione nativa progettata per migliorare radicalmente l'esperienza mobile degli studenti del Politecnico di Milano con la piattaforma WeBeep (Moodle). L'app è costruita con un'architettura **Offline-First**, permettendoti di studiare, consultare documenti e ricevere aggiornamenti ovunque tu sia, anche in metropolitana o senza connessione.

### 🚀 Funzionalità Principali

- 📱 **UI Premium e Moderna**: Un'interfaccia curata nei minimi dettagli, sviluppata interamente in *Jetpack Compose* seguendo gli standard del Material Design 3 (incluso un bellissimo Dark Mode e l'icona Glassmorphism).
- ☁️ **Sincronizzazione in Background**: Non perderti mai un file. L'app usa `WorkManager` per scaricare silenziosamente i nuovi materiali caricati dai professori.
- 🔔 **Notifiche & Smart Deep Linking**: Quando esce un nuovo documento ricevi una notifica push. Premendola, verrai catapultato automaticamente all'interno del corso specifico, dritto al nuovo materiale.
- ⚡ **Motore PDF Custom**: Gestione avanzata dei PDF con rendering asincrono e caching LRU Bitmap. Niente più blocchi o rallentamenti nei file accademici giganti!
- 🔐 **Autenticazione Sicura SSO**: Accesso integrato con i servizi ufficiali del PoliMi tramite Chrome Custom Tabs.

---

### 📥 Download e Installazione

Al momento il progetto è in fase di sviluppo attivo. Puoi scaricare l'ultimo **APK** direttamente dalle Release di GitHub:

<div align="center">
    <a href="https://github.com/alethegoat/WeBeepAndroid/releases/latest">
        <img src="https://img.shields.io/badge/Download_APK-Ultima_Release-2ea44f?style=for-the-badge&logo=android" alt="Download APK" />
    </a>
</div>

#### Compilazione Manuale

1. Clona la repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Apri il progetto in **Android Studio** (consigliata l'ultima versione stabile).
3. Assicurati di avere il JDK 17+ installato.
4. Sincronizza Gradle e premi *Run*.

---

### 🛠 Tech Stack e Architettura

L'applicazione segue i principi della **Clean Architecture** e adotta il pattern **MVI** (Model-View-Intent).

* **Interfaccia Utente**: Jetpack Compose
* **Gestione di Stato e Concorrenza**: Kotlin Coroutines & StateFlow
* **Dependency Injection**: Dagger Hilt
* **Database & Cache**: Room Database & Preferences DataStore
* **Networking**: Retrofit2 & OkHttp

---

> **Disclaimer** <br>
> This is an unofficial application developed by students and is not affiliated with, maintained, or in any way endorsed by Politecnico di Milano. The icon and name are used for identification purposes only. <br>
> *Questa è un'applicazione non ufficiale sviluppata da studenti e non è affiliata, mantenuta o in alcun modo approvata dal Politecnico di Milano. L'icona e il nome sono utilizzati a puro scopo identificativo.*
