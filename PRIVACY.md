# Privacy Policy — WeBeep Android

<div align="center">
  <a href="#-english">🇬🇧 English</a> | <a href="#-italiano">🇮🇹 Italiano</a>
</div>

_Last updated: 2026-09-26_

---

## 🇬🇧 English

WeBeep Android is an **unofficial**, open-source client for the WeBeep (Moodle) platform of Politecnico di Milano. It is not affiliated with or endorsed by Politecnico di Milano. The app has no backend of its own: it talks only to `webeep.polimi.it`, and the developers never receive your data.

### What the app handles

| Data | Why | Where it lives |
|---|---|---|
| Moodle access token (`wstoken`) | Authenticate your requests to WeBeep | On the device only, encrypted (EncryptedSharedPreferences, Android Keystore) |
| Your name and Moodle user ID | Greeting and loading your enrolled courses | On the device only, encrypted together with the token |
| Course list, sections, file names and metadata | Offline access and change detection | Local Room database (app-private) |
| Files you open or download | Offline viewing | App-private internal storage (`files/downloads`) |
| Settings (theme, language, sync frequency) | Your preferences | Local DataStore |

- **Login**: the Politecnico SSO page opens in your browser (Custom Tabs). The app never sees your password. The Moodle "private token" returned by SSO is discarded, not stored.
- **Network**: requests go only to `https://webeep.polimi.it` (HTTPS only). Files you choose to open with another app are shared with that app through a temporary `content://` link.
- **Fonts**: the Outfit and Inter fonts (SIL Open Font License 1.1) are bundled in the app. No font is downloaded at runtime.
- **No analytics, ads or crash reporting**: the app contains no tracking SDKs and sends nothing to the developers or other third parties.
- **Background sync**: WorkManager periodically checks your courses for new files and shows a local notification. Notifications are generated on the device; there is no push server.

### Retention and deletion

- Data is **excluded from cloud backup and device-to-device transfer**.
- **Logging out**, or a session revoked or expired on the server, wipes the token, profile, database, downloaded files and caches.
- Uninstalling the app removes everything it stored.

### Your rights

The data in WeBeep itself (courses, profile) is managed by Politecnico di Milano: exercise your GDPR rights on that data with the University. For questions about this app, open an issue on [GitHub](https://github.com/alethegoat/WeBeepAndroid/issues).

---

## 🇮🇹 Italiano

WeBeep Android è un client **non ufficiale** e open source per la piattaforma WeBeep (Moodle) del Politecnico di Milano. Non è affiliato né approvato dal Politecnico di Milano. L'app non ha un backend proprio: comunica solo con `webeep.polimi.it` e gli sviluppatori non ricevono mai i tuoi dati.

### Dati trattati dall'app

| Dato | Perché | Dove resta |
|---|---|---|
| Token di accesso Moodle (`wstoken`) | Autenticare le richieste a WeBeep | Solo sul dispositivo, cifrato (EncryptedSharedPreferences, Android Keystore) |
| Nome e user ID Moodle | Saluto e caricamento dei corsi a cui sei iscritto | Solo sul dispositivo, cifrati insieme al token |
| Elenco corsi, sezioni, nomi e metadati dei file | Accesso offline e rilevamento delle novità | Database Room locale (privato dell'app) |
| File che apri o scarichi | Visualizzazione offline | Storage interno privato dell'app (`files/downloads`) |
| Impostazioni (tema, lingua, frequenza di sync) | Le tue preferenze | DataStore locale |

- **Login**: la pagina SSO del Politecnico si apre nel browser (Custom Tabs). L'app non vede mai la tua password. Il "private token" Moodle restituito dall'SSO viene scartato, non salvato.
- **Rete**: le richieste vanno solo a `https://webeep.polimi.it` (solo HTTPS). I file che scegli di aprire con un'altra app le vengono passati tramite un link `content://` temporaneo.
- **Font**: i font Outfit e Inter (SIL Open Font License 1.1) sono inclusi nell'app. Nessun font viene scaricato durante l'uso.
- **Niente analytics, pubblicità o crash reporting**: l'app non contiene SDK di tracciamento e non invia nulla agli sviluppatori o ad altre terze parti.
- **Sync in background**: WorkManager controlla periodicamente i corsi e mostra una notifica locale per i file nuovi. Le notifiche sono generate sul dispositivo, senza server push.

### Conservazione e cancellazione

- I dati sono **esclusi dal backup cloud e dal trasferimento tra dispositivi**.
- Il **logout**, o una sessione revocata o scaduta sul server, cancella token, profilo, database, file scaricati e cache.
- Disinstallando l'app viene rimosso tutto ciò che ha salvato.

### I tuoi diritti

I dati presenti su WeBeep (corsi, profilo) sono gestiti dal Politecnico di Milano: per esercitare i diritti GDPR su quei dati rivolgiti all'Ateneo. Per domande su questa app apri una issue su [GitHub](https://github.com/alethegoat/WeBeepAndroid/issues).
