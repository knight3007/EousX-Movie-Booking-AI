# EousX Movie Booking AI

EousX is an AI-assisted cinema movie booking platform built as a university course project and demo-ready MVP. The system includes an Android client for customers, a NestJS API server for business logic, and a React admin web app for cinema staff/admin workflows.

## Main Modules

- `client-app/` - Android customer application.
- `api-server/` - Backend API, authentication, booking, payment, ticket, and AI orchestration.
- `admin-web/` - Admin dashboard for managing and monitoring the cinema flow.
- `docs/` - Project notes, reports, and supporting documentation.

## Tech Stack

### Backend

- NestJS
- Prisma
- PostgreSQL
- JWT authentication
- Firebase Admin for Google login verification
- Gemini API for AI movie assistance

### Android

- Kotlin
- Jetpack Compose
- Retrofit
- Coroutines
- DataStore

### Admin Web

- React
- Vite
- TypeScript
- Tailwind CSS

## Implemented MVP Features

- Email/password authentication
- Google login
- Movie browsing
- Showtime selection
- Seat map and seat locking
- Booking flow
- Mock payment
- Ticket generation and verification
- Booking history
- AI movie assistant
- Admin dashboard and operational pages

## AI Assistant

The Android app does not call Gemini directly. AI requests go through the backend via `/ai/chat`, where the API server controls recommendation logic and calls Gemini when needed.

The AI assistant is designed for the EousX booking context and recommends movies based on movies/showtimes available in the EousX system instead of making unrestricted external recommendations.

## Monorepo Structure

```text
EousX-Movie-Booking-AI/
  api-server/
  client-app/
  admin-web/
  docs/
  README.md
```

## Setup Overview

### API Server

```powershell
cd api-server
npm install
docker compose up -d
npm run prisma:generate
npm run prisma:migrate
npm run seed
npm run start:dev
```

The backend runs without an `/api` prefix. For example, auth routes are under `/auth/*`, not `/api/auth/*`.

### Android Client

Open `client-app/` in Android Studio, configure local files, then build:

```powershell
cd client-app
.\gradlew.bat :app:assembleDebug
```

When using an Android emulator, the backend host should be configured as `http://10.0.2.2:3000/`.

### Admin Web

```powershell
cd admin-web
npm install
npm run dev
```

## Environment and Secrets

The following local files may be required for development, but must not be committed:

- `.env`
- `api-server/firebase-service-account.json`
- `client-app/app/google-services.json`
- `client-app/local.properties`

Use example files or documentation to describe required variables. Never commit API keys, service account JSON files, private keys, or local environment files.

## Demo Flow

```text
Login -> Browse movie -> Select showtime -> Select seats -> Create booking
      -> Mock payment -> Receive ticket -> View booking history
```

Admin demo flow:

```text
Dashboard -> Movies -> Showtimes -> Seat Monitor -> Bookings -> Ticket Check-in
```

## Current MVP Status

- Payment is implemented as mock payment for demo purposes only.
- No production payment gateway is connected yet.
- AI recommendation is an MVP feature controlled by the backend.
- Admin scope is intentionally simplified for a course project/demo workflow.

## Future Scope

- Production payment integration such as ZaloPay or MoMo.
- Realtime seat updates with WebSocket.
- Advanced AI memory and personalized recommendations.
- Admin analytics and reporting.

## Course Note

This project is developed for learning, demonstration, and course evaluation purposes.
