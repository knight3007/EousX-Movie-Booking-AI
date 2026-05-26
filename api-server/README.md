# EousX API Server

NestJS backend for the EousX Movie Booking AI MVP. It handles authentication, movies, showtimes, seat locking, bookings, mock payment, tickets, booking history, admin data, and AI chat orchestration.

The API has no `/api` prefix. Routes are exposed directly, for example `/auth/login`, `/movies`, and `/ai/chat`.

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

```powershell
npm install
docker compose up -d
npm run prisma:generate
npm run prisma:migrate
npm run seed
npm run start:dev
```

Default local API base URL:

```text
http://localhost:3000
```

## Useful Scripts

```powershell
npm run start:dev        # start development server
npm run build            # compile TypeScript
npm run prisma:generate  # generate Prisma client
npm run prisma:migrate   # run development migrations
npm run seed             # seed demo data
npm run test:api         # run API smoke test
```

The smoke test creates demo data and writes diagnostic output to `api-smoke-report.json`, which should not be committed.

## Environment Variables

Create a local `.env` file for development. Required variable names depend on the feature set being used, but commonly include:

```text
DATABASE_URL
JWT_SECRET
FIREBASE_SERVICE_ACCOUNT_PATH
GEMINI_API_KEY
PORT
```

Do not commit `.env`, Firebase service account JSON files, API keys, or private keys.

## Auth Overview

- `POST /auth/register` - email/password registration.
- `POST /auth/login` - email/password login.
- `POST /auth/google` - verifies a Firebase ID token and returns an EousX backend JWT.
- `GET /auth/me` - returns the current authenticated user.

Customer protected routes use:

```text
Authorization: Bearer <EOUSX_BACKEND_JWT>
```

Firebase ID tokens are only used for Google login. After login, clients should use the EousX backend JWT.

## Google Login Flow

```text
Android Google Sign-In
-> Firebase ID token
-> POST /auth/google
-> Firebase Admin verification
-> EousX backend JWT
-> Android uses backend JWT for protected APIs
```

## Major Endpoint Areas

- `/auth/*` - authentication and current user.
- `/movies/*` - movie listing and details.
- `/showtimes/*` - showtimes and seat availability.
- `/seat-locks/*` - temporary seat locking.
- `/bookings/*` - booking creation and booking history.
- `/payments/*` - mock payment flow for MVP demo.
- `/tickets/*` - ticket lookup, verification, and check-in.
- `/admin/*` - admin-side movie/showtime operations.
- `/ai/chat` - AI movie assistant endpoint.

## AI Chat

Android and admin clients should not call Gemini directly. AI chat requests go through:

```text
POST /ai/chat
```

The backend controls recommendation context and keeps suggestions aligned with movies/showtimes available in EousX.

## Security Notes

- Never commit `.env`.
- Never commit `firebase-service-account.json`.
- Never expose `GEMINI_API_KEY`, JWT secrets, database credentials, or private keys.
- Use `.env.example` for documenting variable names only.
