# EousX Android Client

Android customer app for the EousX Movie Booking AI MVP. The app supports authentication, movie browsing, showtime selection, seat booking, mock payment, tickets, booking history, and AI chat integration through the backend.

## Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- Retrofit
- Kotlin Coroutines
- DataStore
- Firebase Authentication for Google login
- Gradle Android project with module `:app`

## Architecture

The app follows a Compose-first structure with screen-level UI, ViewModels, repository/data layers, and Retrofit APIs for backend communication.

The Android client is integrated with the EousX backend only:

- It does not call Gemini directly.
- It does not use TMDB directly anymore.
- AI chat is sent to the backend through `/ai/chat`.
- Backend routes do not use an `/api` prefix.

## Backend Host

For Android emulator development, use:

```text
http://10.0.2.2:3000/
```

For a physical Android device, use the LAN IP address of the machine running the backend, for example:

```text
http://192.168.x.x:3000/
```

## Current MVP Features

- Login and register
- Google login
- Movie browsing from the EousX backend
- Showtime selection
- Seat map
- Seat locking
- Booking creation
- Mock payment
- Ticket display
- Booking history
- AI chat integration

## Required Local Files

These files are required locally but must not be committed:

```text
local.properties
app/google-services.json
```

`local.properties` should contain local Android/backend configuration as needed. `google-services.json` is required for Firebase/Google login.

## Setup

1. Open `client-app/` in Android Studio.
2. Sync Gradle.
3. Add the required local files.
4. Start the backend server.
5. Run the Android app on an emulator or physical device.

Build from PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

## AI Chat Integration

The app sends chat/recommendation requests to the backend. Gemini keys and recommendation rules are controlled server-side, so no Gemini API key should be stored in the Android app.

## Security Notes

- Do not commit `local.properties`.
- Do not commit `app/google-services.json`.
- Do not store backend secrets, Gemini keys, service account files, or private keys in the Android source tree.
