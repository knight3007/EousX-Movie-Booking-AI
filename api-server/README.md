# EousX API Server

Backend server cho hệ thống đặt vé xem phim EousX.

## Stack

- Node.js
- TypeScript
- NestJS
- PostgreSQL
- Prisma
- Docker

## Chạy database

```bash
docker compose up -d
```

## Auth

Backend routes run without `/api` prefix.

- `POST /auth/register`: local email/password register.
- `POST /auth/login`: local email/password login.
- `POST /auth/google`: login/register with a Firebase ID Token from Android Google Sign-In, then returns an EousX backend JWT.
- `GET /auth/me`: current user from EousX backend JWT.
