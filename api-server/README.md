# EousX API Server

NestJS backend for the EousX Movie Booking AI MVP. It handles authentication, movies, rooms, showtimes, seat maps, seat locking, bookings, payment (SePay Test Mode and mock-success fallback), tickets, admin data, and AI chat orchestration.

The API has no `/api` prefix. Routes are exposed directly, for example `/auth/login`, `/movies`, `/bookings`, and `/ai/chat`.

See the root README for course/team information.

## Stack

- Node.js + TypeScript
- NestJS
- Prisma
- PostgreSQL
- JWT
- Firebase Admin
- Gemini API
- Docker Compose for local PostgreSQL

## Setup

Copy the environment template, then start PostgreSQL and the API together:

```powershell
Copy-Item .env.example .env
docker compose up --build --detach --wait
```

Base URL:

```text
http://localhost:3000
```

For a fresh demo database, seed the sample data once:

```powershell
docker compose exec api npm run seed
```

The seed script deletes existing application data before recreating the demo dataset. Do not run it against a database you need to preserve.

Useful container commands:

```powershell
docker compose ps
docker compose logs --follow api
docker compose down
```

The API container waits for PostgreSQL to become healthy, applies pending Prisma migrations, and then starts the compiled NestJS server. To run the API directly on the host for development, start only PostgreSQL and use the existing npm scripts:

```powershell
npm ci
docker compose up --detach postgres
npx prisma generate
npx prisma migrate dev
npm run start:dev
```

## Useful Scripts

```powershell
npm run start:dev        # start development server
npm run build            # compile TypeScript
npm run prisma:generate  # generate Prisma client
npm run prisma:migrate   # run development migration command
npm run seed             # seed demo data
npm run test:api         # run API smoke test
```

The smoke test defaults to `http://localhost:3000` (override via `EOUSX_API_BASE_URL` for remote testing). It creates temporary test data and outputs `api-smoke-report.json`, which is ignored by Git.

## Environment Variables

Copy `.env.example` to `.env` and configure local values:

```powershell
cp .env.example .env
```

Key environment variable groups:

| Group | Variables | Purpose |
|---|---|---|
| Server | `PORT`, `API_HOST_PORT`, `NODE_ENV` | API listen port, Compose host-port mapping, and environment mode. |
| Database | `DATABASE_URL` | PostgreSQL connection string used by Prisma. |
| Auth | `JWT_SECRET`, `JWT_EXPIRES_IN` | JWT signing secret and token expiration TTL. |
| Firebase | `FIREBASE_SERVICE_ACCOUNT_PATH`, `FIREBASE_SERVICE_ACCOUNT_JSON`, `FIREBASE_TEST_ID_TOKEN` | Service account for Firebase Admin SDK Google auth verification. |
| Gemini AI | `GEMINI_API_KEY` | Backend-only API key for the Gemini movie assistant (`POST /ai/chat`). |
| SePay Payment | `SEPAY_WEBHOOK_API_KEY`, `SEPAY_QR_BASE_URL`, `SEPAY_BANK_CODE`, `SEPAY_BANK_ACCOUNT_NUMBER`, `SEPAY_ACCOUNT_NAME` | VietQR / SePay payment configuration and webhook validation key. |

`GEMINI_API_KEY` is required only for real `POST /ai/chat` responses. The key belongs on the backend only; Android and admin web must not store or call Gemini keys directly.

Do not commit `.env`, Firebase service account JSON files, API keys, JWT secrets, database credentials, or private keys.

## API Route Overview

### Health

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/health` | Server health check. |

### Auth

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/auth/register` | Email/password registration. |
| `POST` | `/auth/login` | Email/password login. |
| `POST` | `/auth/google` | Verify Firebase ID token and return EousX backend JWT. |
| `GET` | `/auth/me` | Get current authenticated user. |

### Movies

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/movies` | List movies. |
| `GET` | `/movies/now-showing` | List movies with `NOW_SHOWING` status. |
| `GET` | `/movies/upcoming` | List movies with `UPCOMING` status. |
| `GET` | `/movies/:id` | Get movie detail. |

### Rooms

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/rooms` | List rooms. |
| `GET` | `/rooms/:id` | Get room with seats. |
| `GET` | `/admin/rooms` | Admin room list. |
| `GET` | `/admin/rooms/:id` | Admin room detail. |

### Showtimes

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/showtimes` | List showtimes, optionally filtered by `date`. |
| `GET` | `/showtimes/:id` | Get showtime detail. |
| `GET` | `/movies/:movieId/showtimes` | List showtimes for a movie. |
| `GET` | `/movies/:movieId/showtimes?date=YYYY-MM-DD` | List movie showtimes for a date. |
| `GET` | `/admin/showtimes` | Admin showtime list, optionally filtered by `date`. |
| `POST` | `/admin/showtimes` | Create showtime. |
| `PUT` | `/admin/showtimes/:id` | Update showtime. |
| `DELETE` | `/admin/showtimes/:id` | Cancel showtime. |

### Seats and Seat Map

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/showtimes/:showtimeId/seats` | Customer seat map for a showtime. |
| `GET` | `/admin/showtimes/:showtimeId/seats` | Admin seat monitor data for a showtime. |

Seat display status is calculated for a specific showtime from active locks and paid/checked-in bookings. It is not stored globally as a permanent `SOLD` field on `Seat`.

### Seat Locks

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/showtimes/:showtimeId/seat-locks` | Lock selected seats for the authenticated user. |
| `GET` | `/seat-locks/:lockId` | Get seat lock detail. |
| `DELETE` | `/seat-locks/:lockId` | Release a lock owned by the authenticated user. |

### Bookings

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/bookings` | Create booking from lock IDs. |
| `GET` | `/bookings/me` | Current user's booking history. |
| `GET` | `/bookings/:id` | Current user's booking detail. |
| `PATCH` | `/bookings/:id/cancel` | Cancel current user's unpaid booking. |
| `GET` | `/admin/bookings` | Admin booking list. |
| `GET` | `/admin/bookings/:id` | Admin booking detail. |

### Payments

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/payments/sepay/create` | Create SePay payment and return QR URL. JWT required. |
| `POST` | `/payments/webhook/sepay` | SePay webhook endpoint. Protected by SePay API Key (not JWT). |
| `GET` | `/payments/sepay/status/:paymentCode` | Poll SePay payment and booking status by paymentCode. JWT required. |
| `POST` | `/payments/mock-success` | Mark a waiting booking as paid instantly (demo/fallback). JWT required. |
| `GET` | `/payments/:id/status` | Get payment status/detail. |
| `GET` | `/bookings/:bookingId/payment` | Get current user's payment by booking. |

### Tickets

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/bookings/:bookingId/ticket` | Get current user's ticket for a booking. |
| `GET` | `/tickets/verify/:qrCode` | Verify ticket QR code for staff/admin. |
| `POST` | `/tickets/:ticketId/check-in` | Mark a valid paid ticket as used. |

### AI

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/ai/chat` | Authenticated AI movie assistant. |

### Admin Movies

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/admin/movies` | Create movie. |
| `PUT` | `/admin/movies/:id` | Update movie. |
| `DELETE` | `/admin/movies/:id` | Delete movie. |

## Auth Rules

- Customer protected APIs require `Authorization: Bearer <EOUSX_BACKEND_JWT>`.
- Android/client must not send `userId` for protected customer APIs.
- `userId` is extracted from the backend JWT.
- Firebase ID token is used only for `POST /auth/google`.
- After Google login succeeds, clients use the EousX backend JWT for bookings, seat locks, payments, tickets, and AI chat.

## AI Rules

- Android does not call Gemini directly.
- `/ai/chat` is protected by JWT.
- The backend builds `AVAILABLE_MOVIES` from real `OPEN` showtimes in the selected time range.
- Gemini output is parsed and sanitized.
- Recommendations are limited to movies in `AVAILABLE_MOVIES`; invalid or invented movie IDs are discarded.
- If Gemini fails or returns unusable output, the backend returns a fallback recommendation from available showtimes.

## Database Model Overview

| Model | Summary |
|---|---|
| `User` | Customer account, email/password or Firebase UID, owns bookings and seat locks. |
| `Movie` | Movie metadata, status, genres, poster/backdrop/trailer fields, showtimes. |
| `Cinema` | Cinema entity containing rooms. |
| `Room` | Screening room with type, row/column dimensions, seats, and showtimes. |
| `Seat` | Physical seat in a room with row, number, code, type, and active flag. |
| `Showtime` | Screening of a movie in a room, with start/end time, base price, and status. |
| `SeatLock` | Temporary seat reservation for a user/showtime/seat. |
| `Booking` | Customer booking for a showtime with status and total amount. |
| `BookingSeat` | Seats and prices attached to a booking. |
| `Payment` | One payment record per booking. |
| `Ticket` | One ticket per booking with QR code and check-in status. |

Main relationships:

- `Showtime` belongs to `Movie` and `Room`.
- `Seat` belongs to `Room`.
- `Booking` belongs to `User` and `Showtime`.
- `Payment` and `Ticket` each belong to one `Booking`.
- Seat availability is derived per showtime from active `SeatLock` records and paid/checked-in `BookingSeat` records.

## Status and Enum Reference

| Enum | Values | Meaning |
|---|---|---|
| `RoomType` | `STANDARD_2D`, `VIP`, `COUPLE` | Room category. |
| `SeatType` | `STANDARD`, `VIP`, `COUPLE`, `DISABLED`, `MAINTENANCE` | Physical seat category or unavailable maintenance type. |
| `MovieStatus` | `NOW_SHOWING`, `UPCOMING`, `ENDED` | Movie lifecycle in the cinema. |
| `ShowtimeStatus` | `OPEN`, `CLOSED`, `CANCELLED` | Showtime availability. |
| `SeatLockStatus` | `ACTIVE`, `EXPIRED`, `RELEASED`, `CONVERTED_TO_BOOKING` | Temporary lock lifecycle. |
| `BookingStatus` | `PENDING`, `WAITING_PAYMENT`, `PAID`, `EXPIRED`, `CANCELLED`, `REFUNDED`, `CHECKED_IN` | Booking lifecycle. |
| `PaymentStatus` | `CREATED`, `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `EXPIRED` | Payment lifecycle. |
| `TicketStatus` | `VALID`, `USED`, `CANCELLED`, `EXPIRED` | Ticket lifecycle. |
| `AdminRole` | `ADMIN`, `MANAGER`, `STAFF` | Admin role model present in schema. |

Runtime seat map display statuses:

- `AVAILABLE` - selectable.
- `LOCKED` - temporarily locked by an active seat lock.
- `SOLD` - belongs to a paid or checked-in booking for the showtime.
- `MAINTENANCE` - inactive/maintenance seat.

## MVP Notes

- Payment supports two flows: SePay Test Mode (QR + server-side webhook confirmation) and mock-success for quick demo. End-to-end SePay requires the SePay test environment and a public webhook endpoint (e.g. Cloudflare Tunnel).
- Admin routes are public in the current demo step; the `Admin` and `AdminRole` schema models are present, but admin login/guards are not wired into these routes yet.
- `DELETE /admin/showtimes/:id` cancels a showtime instead of physically deleting it.
- Production hardening should add admin authentication/authorization and SePay Production Mode.
