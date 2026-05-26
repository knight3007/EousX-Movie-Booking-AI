# EousX Movie Booking AI

EousX is an AI-assisted cinema movie booking platform built as a university course project and demo-ready MVP. It contains an Android customer app, a NestJS API server, and a React admin dashboard in one monorepo.

## Project Information

- University: University of Information Technology
- Builders:
  - 23520450 - Đỗ Thái Hậu
  - 23520297 - Hoàng Xuân Đồng

## Monorepo Structure

```text
EousX-Movie-Booking-AI/
  api-server/   # NestJS API, Prisma, PostgreSQL, auth, booking, AI
  client-app/   # Android customer app
  admin-web/    # React admin dashboard
  docs/         # Supporting project notes and reports
  README.md
```

## Main Modules

| Module | Purpose |
|---|---|
| `api-server/` | Business logic, JWT auth, Firebase Google login verification, movie/showtime/seat/booking/payment/ticket APIs, AI chat orchestration. |
| `client-app/` | Android customer flow: login, browse movies, choose showtime/seats, mock payment, ticket, history, AI chat. |
| `admin-web/` | Admin/operator flow: dashboard, movies, showtimes, seat monitor, bookings, ticket check-in. |

## Tech Stack

| Area | Stack |
|---|---|
| Backend | NestJS, TypeScript, Prisma, PostgreSQL, JWT, Firebase Admin, Gemini API |
| Android | Kotlin, Jetpack Compose, Hilt, Retrofit/Moshi/OkHttp, Coroutines/StateFlow, DataStore, Firebase Auth |
| Admin Web | React, Vite, TypeScript, Tailwind CSS, Axios, React Router, TanStack Query |

## Implemented MVP Features

- Email/password register and login
- Google login through Firebase ID token verification on the backend
- Movie browsing and movie details
- Showtime selection
- Seat map and temporary seat locking
- Booking creation and booking history
- Mock payment flow
- Ticket generation, verification, and check-in
- AI movie assistant through backend `/ai/chat`
- Admin dashboard for the demo cinema flow

## Architecture Flow

```text
Android App / Admin Web
        |
        v
NestJS API Server  -- Prisma --> PostgreSQL
        |
        +-- Firebase Admin verifies Google login tokens
        |
        +-- Gemini API powers AI chat recommendations
```

The backend has no `/api` prefix. Example routes are `/auth/login`, `/movies`, `/bookings`, and `/ai/chat`.

## AI Assistant

- Android does not call Gemini directly.
- Clients call `POST /ai/chat` with the EousX backend JWT.
- The backend builds an `AVAILABLE_MOVIES` context from real `OPEN` showtimes.
- Gemini recommendations are sanitized by the backend so only movies available in EousX showtimes are returned.

## API Overview

| Group | Example routes |
|---|---|
| Health | `GET /health` |
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/google`, `GET /auth/me` |
| Movies | `GET /movies`, `GET /movies/now-showing`, `GET /movies/upcoming`, `GET /movies/:id` |
| Rooms | `GET /rooms`, `GET /rooms/:id` |
| Showtimes | `GET /showtimes`, `GET /showtimes/:id`, `GET /movies/:movieId/showtimes?date=YYYY-MM-DD` |
| Seats | `GET /showtimes/:showtimeId/seats`, `GET /admin/showtimes/:showtimeId/seats` |
| Seat Locks | `POST /showtimes/:showtimeId/seat-locks`, `GET /seat-locks/:lockId`, `DELETE /seat-locks/:lockId` |
| Bookings | `POST /bookings`, `GET /bookings/me`, `GET /bookings/:id`, `PATCH /bookings/:id/cancel` |
| Payments | `POST /payments/mock-success`, `GET /payments/:id/status`, `GET /bookings/:bookingId/payment` |
| Tickets | `GET /bookings/:bookingId/ticket`, `GET /tickets/verify/:qrCode`, `POST /tickets/:ticketId/check-in` |
| AI | `POST /ai/chat` |
| Admin | `/admin/movies`, `/admin/showtimes`, `/admin/bookings`, `/admin/rooms` |

## Database Overview

Core Prisma models:

- `User` owns customer bookings and seat locks.
- `Movie` has many showtimes.
- `Cinema` has rooms; `Room` has seats and showtimes.
- `Seat` belongs to a room. Sold/locked display status is calculated per showtime from bookings and active locks.
- `Showtime` belongs to a movie and room.
- `SeatLock` temporarily reserves a seat for a user and showtime.
- `Booking` belongs to a user and showtime.
- `BookingSeat` stores seats and prices for a booking.
- `Payment` belongs to one booking.
- `Ticket` belongs to one booking and stores the QR code/check-in state.

## Status Labels

| Enum | Values |
|---|---|
| `RoomType` | `STANDARD_2D`, `VIP`, `COUPLE` |
| `MovieStatus` | `NOW_SHOWING`, `UPCOMING`, `ENDED` |
| `ShowtimeStatus` | `OPEN`, `CLOSED`, `CANCELLED` |
| `SeatType` | `STANDARD`, `VIP`, `COUPLE`, `DISABLED`, `MAINTENANCE` |
| Seat display status | `AVAILABLE`, `LOCKED`, `SOLD`, `MAINTENANCE` |
| `SeatLockStatus` | `ACTIVE`, `EXPIRED`, `RELEASED`, `CONVERTED_TO_BOOKING` |
| `BookingStatus` | `PENDING`, `WAITING_PAYMENT`, `PAID`, `EXPIRED`, `CANCELLED`, `REFUNDED`, `CHECKED_IN` |
| `PaymentStatus` | `CREATED`, `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `EXPIRED` |
| `TicketStatus` | `VALID`, `USED`, `CANCELLED`, `EXPIRED` |
| `AdminRole` | `ADMIN`, `MANAGER`, `STAFF` |

## Setup Overview

### API Server

```powershell
cd api-server
npm install
docker compose up -d
npx prisma generate
npx prisma migrate dev
npx prisma db seed
npm run start:dev
```

### Android Client

Open `client-app/` in Android Studio, configure local files, then build:

```powershell
cd client-app
.\gradlew.bat :app:assembleDebug
```

For an Android emulator, use backend base URL `http://10.0.2.2:3000/`.

### Admin Web

```powershell
cd admin-web
npm install
npm run dev
```

## Environment and Security

Local development may require:

- `api-server/.env`
- `api-server/firebase-service-account.json`
- `client-app/local.properties`
- `client-app/app/google-services.json`
- `admin-web/.env`

These files must not be committed. The root `.gitignore` is configured to ignore local environment files, Firebase service accounts, Google services JSON, private keys, build outputs, and dependency folders.

## Demo Flow

Customer:

```text
Login -> Browse movie -> Movie detail -> Showtime -> Seat map
      -> Booking -> Mock payment -> Ticket -> Booking history
```

Admin:

```text
Dashboard -> Movies -> Showtimes -> Seat Monitor -> Bookings -> Ticket Check-in
```

## Current MVP Status

- Payment is mock-only for demo purposes.
- Seat monitor uses polling in the admin web app.
- Admin routes are public in the current demo scope; admin authentication/authorization is future scope.
- Android ticket screen displays the QR code string returned by the backend; generated QR image rendering is future scope.
- Android uses the EousX backend as the movie source of truth. Movie metadata, trailer keys, posters, and backdrops should be managed by backend/admin data.

## Future Scope

- Production payment gateway such as ZaloPay or MoMo.
- Realtime seat updates with WebSocket.
- Advanced AI memory and stronger personalization.
- Admin authentication and role-based permissions.
- Analytics and reporting.

## Course Note

This project is developed for learning, demonstration, and course evaluation purposes.
