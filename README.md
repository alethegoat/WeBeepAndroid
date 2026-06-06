<div align="center">
  <img src="docs/icon.png" width="200" alt="WeBeep App Icon">
  
  # WeBeep Android
  
  **Il client nativo per Android pensato per gli studenti del Politecnico di Milano.**
  
  ![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?logo=kotlin)
  ![Android](https://img.shields.io/badge/Android-8.0+-green.svg?logo=android)
  ![Jetpack Compose](https://img.shields.io/badge/Compose-Material_3-4285F4.svg?logo=android)
  ![License](https://img.shields.io/github/license/alethegoat/WeBeepAndroid)
</div>

---

### 🇮🇹 Cos'è esattamente?

WeBeep Android è un'applicazione nativa progettata per migliorare radicalmente l'esperienza mobile degli studenti del Politecnico di Milano con la piattaforma WeBeep (Moodle). L'app è costruita con un'architettura **Offline-First**, permettendoti di studiare, consultare documenti e ricevere aggiornamenti ovunque tu sia, anche in metropolitana o senza connessione.

### 🚀 Funzionalità Principali

- 📱 **UI Premium e Moderna**: Un'interfaccia curata nei minimi dettagli, sviluppata interamente in *Jetpack Compose* seguendo gli standard del Material Design 3 (incluso un bellissimo Dark Mode e l'icona Glassmorphism).
- ☁️ **Sincronizzazione in Background**: Non perderti mai un file. L'app usa `WorkManager` per scaricare silenziosamente i nuovi materiali caricati dai professori.
- 🔔 **Notifiche & Smart Deep Linking**: Quando esce un nuovo documento ricevi una notifica push. Premendola, verrai catapultato automaticamente all'interno del corso specifico, dritto al nuovo materiale.
- ⚡ **Motore PDF Custom**: Gestione avanzata dei PDF con rendering asincrono e caching LRU Bitmap. Niente più blocchi o rallentamenti nei file accademici giganti!
- 🔐 **Autenticazione Sicura SSO**: Accesso integrato con i servizi ufficiali del PoliMi tramite Chrome Custom Tabs.

---

## 📥 Download e Installazione

Al momento il progetto è in fase di sviluppo attivo. Puoi scaricare l'ultimo **APK** direttamente dalle Release di GitHub:

<div align="center">
    <a href="https://github.com/alethegoat/WeBeepAndroid/releases/latest">
        <img src="https://img.shields.io/badge/Download_APK-Ultima_Release-2ea44f?style=for-the-badge&logo=android" alt="Download APK" />
    </a>
</div>

### Compilazione Manuale

1. Clona la repository:
   ```bash
   git clone https://github.com/alethegoat/WeBeepAndroid.git
   ```
2. Apri il progetto in **Android Studio** (consigliata l'ultima versione stabile).
3. Assicurati di avere il JDK 17+ installato.
4. Sincronizza Gradle e premi *Run*.

---

## 🛠 Tech Stack e Architettura

L'applicazione segue i principi della **Clean Architecture** e adotta il pattern **MVI** (Model-View-Intent).

* **Interfaccia Utente**: Jetpack Compose
* **Gestione di Stato e Concorrenza**: Kotlin Coroutines & StateFlow
* **Dependency Injection**: Dagger Hilt
* **Database & Cache**: Room Database & Preferences DataStore
* **Networking**: Retrofit2 & OkHttp

---

> **Disclaimer** <br>
> Questa è un'applicazione non ufficiale sviluppata da studenti e non è affiliata, mantenuta o in alcun modo approvata dal Politecnico di Milano. L'icona e il nome sono utilizzati a puro scopo identificativo.
