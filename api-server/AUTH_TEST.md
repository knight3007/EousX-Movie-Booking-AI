# Auth API Test

Backend does not use `/api` prefix. Admin Web routes are still public for this step. Customer APIs require a Bearer token.

## 1. Register

POST `http://localhost:3000/auth/register`

```json
{
  "fullName": "Test User",
  "email": "test@eousx.com",
  "password": "123456",
  "phone": "0900000001"
}
```

## 2. Login

POST `http://localhost:3000/auth/login`

```json
{
  "email": "test@eousx.com",
  "password": "123456"
}
```

## 3. Me

GET `http://localhost:3000/auth/me`

Header: `Authorization: Bearer TOKEN`

## Google login via Firebase ID Token

Flow:

Android Google Sign-In / Firebase Auth -> Android gets Firebase ID Token -> Android sends ID Token to API server -> API server verifies token using Firebase Admin SDK -> API server finds or creates User -> API server returns EousX backend JWT -> Android uses EousX JWT for booking APIs.

POST `http://localhost:3000/auth/google`

```json
{
  "idToken": "FIREBASE_ID_TOKEN_FROM_ANDROID"
}
```

Response:

```json
{
  "message": "Google login successfully",
  "accessToken": "EOUSX_BACKEND_JWT",
  "user": {
    "id": "...",
    "fullName": "...",
    "email": "...",
    "phone": null,
    "avatarUrl": "..."
  }
}
```

Backend does not use `/api` prefix. Firebase ID Token is only used for `POST /auth/google`. After successful login, Android must use the backend `accessToken`; booking, seat-lock, and payment APIs do not accept Firebase tokens directly.

Local Firebase Admin setup:

1. Open Firebase Console.
2. Enable Authentication -> Sign-in method -> Google.
3. Download the Admin SDK service account JSON.
4. Put the file at `api-server/firebase-service-account.json`.
5. Add this to `.env`:

```env
FIREBASE_SERVICE_ACCOUNT_PATH="./firebase-service-account.json"
```

Do not commit service account JSON or private keys. If Firebase Admin is not configured, `/auth/google` returns a configuration error, while local email/password auth and the rest of the API continue to run normally.

## 4. Lock seats

POST `http://localhost:3000/showtimes/SHOWTIME_ID/seat-locks`

Header: `Authorization: Bearer TOKEN`

```json
{
  "seatIds": ["SEAT_ID_1", "SEAT_ID_2"]
}
```

## 5. Create booking

POST `http://localhost:3000/bookings`

Header: `Authorization: Bearer TOKEN`

```json
{
  "showtimeId": "SHOWTIME_ID",
  "lockIds": ["LOCK_ID_1", "LOCK_ID_2"]
}
```

## 6. My bookings

GET `http://localhost:3000/bookings/me`

Header: `Authorization: Bearer TOKEN`

## 7. Mock payment

POST `http://localhost:3000/payments/mock-success`

Header: `Authorization: Bearer TOKEN`

```json
{
  "bookingId": "BOOKING_ID",
  "provider": "MOCK"
}
```

## 8. Get ticket

GET `http://localhost:3000/bookings/BOOKING_ID/ticket`

Header: `Authorization: Bearer TOKEN`

## 9. Verify ticket for staff/admin web

GET `http://localhost:3000/tickets/verify/QR_CODE`

## 10. Check-in ticket for staff/admin web

POST `http://localhost:3000/tickets/TICKET_ID/check-in`

## Automated API smoke test

Backend does not use `/api` prefix.

1. Start database:

```bash
docker compose up -d
```

2. Start API server:

```bash
npm run start:dev
```

3. Open another terminal and run:

```bash
npm run test:api
```

Use a different base URL if needed.

PowerShell:

```powershell
$env:EOUSX_API_BASE_URL="http://localhost:3000"
npm run test:api
```

CMD:

```cmd
set EOUSX_API_BASE_URL=http://localhost:3000
npm run test:api
```

The script creates a new test user, movie, showtime, booking, payment, and ticket on each run using a timestamp suffix. It does not delete paid or checked-in bookings because those records are used to verify admin dashboard data. Test data may remain in the database, but names/emails use a timestamp suffix so they do not collide.

Smoke test data is real database data. The script automatically searches for an empty future showtime slot before creating test showtimes, so it can be run repeatedly without failing because of old smoke-test schedules. If the database accumulates too much smoke data, reset and seed the database again or clean records with the `Smoke Test` / `Smoke Delete` name prefix.

If a test fails, inspect `api-smoke-report.json` for the endpoint, response status, body, and created IDs.

To run real `/auth/google` in the smoke test, set `FIREBASE_SERVICE_ACCOUNT_PATH` and provide a valid `FIREBASE_TEST_ID_TOKEN`. Without `FIREBASE_TEST_ID_TOKEN`, the Google login smoke test is skipped and the main suite can still be `STABLE`.
