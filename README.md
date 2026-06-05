# WeBeep Android

I built this native Android client for Politecnico di Milano's WeBeep (Moodle) because the web version was getting quite slow on mobile, especially when trying to read big slide decks on the go. The main goal here was to make everything work offline so you don't need a connection to study on the metro.

### What it does

- **Works offline:** It caches courses and downloaded files using Room. You can basically use the app without internet once it syncs.
- **SSO Login:** It hooks right into Polimi's SSO using Chrome Custom Tabs, so login is straightforward.
- **Fast PDF rendering:** I spent a lot of time tweaking the PDF engine. It uses background pre-fetching and a custom Bitmap pool so you can scroll through heavy slides at 60fps without stuttering.
- **Notifications:** There's a WorkManager job that checks the server periodically. If a professor uploads something new, you get a system notification.
- **UI:** Everything is built from scratch with Jetpack Compose (Material 3, dark mode included).

### Tech Details

If you want to poke around the code, the architecture is based on MVI and Clean Architecture. I used Coroutines and StateFlow for state management. For dependency injection, it's using Dagger Hilt. Network calls are handled by Retrofit and OkHttp.

### How to build

1. Clone the repo
2. Open it up in Android Studio (I'm using Koala, but newer versions should be fine).
3. Make sure you have JDK 17.
4. Let Gradle sync and hit run.

### Disclaimer

Just to be clear, this is a completely unofficial app. I made it for myself and other students, it's not endorsed or maintained by Politecnico di Milano.
