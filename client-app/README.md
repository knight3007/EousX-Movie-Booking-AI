# EousX Android Client

Android customer app for the EousX Movie Booking AI MVP. It provides the customer-facing flow for authentication, movie discovery, showtime selection, seat locking, booking, mock payment, tickets, booking history, profile/logout, and AI chat.

See the root README for course/team information.

## Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- Hilt
- Retrofit, Moshi, OkHttp
- Coroutines and StateFlow
- DataStore
- Firebase Auth for Google login
- Gradle Android module `:app`

## Backend Integration Rules

- The current MVP customer flow uses the EousX backend as the movie source of truth.
- Backend base URL has no `/api` prefix.
- Android does not call Gemini directly.
- Android sends AI messages to `POST /ai/chat`.
- Android does not send `userId` for protected customer APIs; the backend extracts it from the JWT.
- Firebase ID token is used only for Google login; the app stores and uses the EousX backend JWT after login.

Movie metadata, trailer keys, posters, and backdrops should come from backend/admin data. Android does not use TMDB directly.

## Backend Base URL

Android emulator:

```text
http://10.0.2.2:3000/
```

Physical Android device:

```text
http://<your-computer-lan-ip>:3000/
```

## Current Routes and Screens

| Screen | Route | Purpose |
|---|---|---|
| Splash | `splash_screen` | Checks saved backend JWT and calls `GET /auth/me`. |
| Login | `login_screen` | Email/password login and Google login entry. |
| Register | `register_screen` | Email/password registration. |
| Home | `home_screen` | Movie browsing, booking history/profile tabs, AI entry, logout. |
| Movie Detail | `detail_screen/{movieId}` | Movie details and booking entry. |
| Movie Schedule | `movie_schedule/{movieId}` | Showtime selection. |
| Seat Map | `seat_map/{showtimeId}` | Seat display and seat lock request. |
| Checkout | `checkout/{showtimeId}` | Booking confirmation from lock IDs. |
| Payment | `payment/{bookingId}` | Mock payment flow. |
| Ticket | `ticket/{bookingId}` | Ticket details and QR code string. |
| AI Chat | `ai_chat` | AI assistant with recommended movie cards. |

## MVP Features

- Splash session check
- Login and register
- Google login through Firebase Auth and backend `/auth/google`
- Home movie list from backend
- Movie detail
- Trailer open action to YouTube/browser when trailer data is available
- Showtime selection
- Seat map with `AVAILABLE`, `LOCKED`, `SOLD`, and `MAINTENANCE` states
- Seat locking and release
- Checkout and booking creation
- Mock payment
- Ticket detail
- Booking history
- Profile/logout
- AI chat integration

## Backend API Usage

The app uses `BackendApi` for:

- `POST auth/register`
- `POST auth/login`
- `POST auth/google`
- `GET auth/me`
- `GET movies`
- `GET movies/{id}`
- `GET movies/{movieId}/showtimes`
- `GET showtimes/{showtimeId}/seats`
- `POST showtimes/{showtimeId}/seat-locks`
- `DELETE seat-locks/{lockId}`
- `POST bookings`
- `GET bookings/me`
- `GET bookings/{id}`
- `PATCH bookings/{id}/cancel`
- `POST payments/mock-success`
- `GET bookings/{bookingId}/ticket`
- `POST ai/chat`

## Session Behavior

- `TokenManager` stores the EousX backend access token in DataStore.
- `AuthInterceptor` attaches `Authorization: Bearer <token>` to backend requests when a token exists.
- Splash checks the saved token and validates it with `GET /auth/me`.
- If validation fails, the app logs out and returns to login.
- Logout clears the backend token and also signs out Firebase/Google where applicable.

## AI Chat Behavior

- Calls `POST /ai/chat` through the backend.
- Renders AI replies and recommended movie cards.
- Recommended movie cards can open movie detail.
- Booking action opens showtime selection for the recommended movie.
- Gemini API keys are never stored in the Android app.

## Required Local Files

These files are required locally but must not be committed:

```text
local.properties
app/google-services.json
```

`google-services.json` is required for Firebase/Google login. `local.properties` is for local machine configuration.

## Setup and Build

1. Open `client-app/` in Android Studio.
2. Sync Gradle.
3. Add required local files.
4. Start the backend API server.
5. Run on an emulator or physical device.

PowerShell build command:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Known Limitations and Future Scope

- Payment is mock-only; no production payment gateway is connected yet.
- No WebSocket realtime seat updates yet.
- Ticket screen currently displays the backend QR code string; generated QR image rendering is future scope.
- Movie metadata/trailer/poster management belongs to backend/admin data, not a direct Android TMDB integration.

## Security Notes

- Do not commit `local.properties`.
- Do not commit `app/google-services.json`.
- Do not store backend secrets, Gemini keys, Firebase service account files, or private keys in the Android source tree.
