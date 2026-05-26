# EousX Admin Web

React admin dashboard for the EousX Movie Booking AI MVP. This app is used by the cinema/admin side to manage demo data, monitor seats, review bookings, and check in tickets.

## Stack

- React
- Vite
- TypeScript
- Tailwind CSS
- React Router
- TanStack Query
- Axios
- Lucide React

## Setup

```powershell
npm install
npm run dev
```

Build for production:

```powershell
npm run build
```

Preview a production build:

```powershell
npm run preview
```

## Backend Base URL

The admin web app talks to the EousX API server. The backend has no `/api` prefix.

Typical local backend URL:

```text
http://localhost:3000
```

Configure the API base URL through the local environment file used by the app, for example:

```text
VITE_API_BASE_URL=http://localhost:3000
```

Do not commit local `.env` files.

## MVP Pages and Features

- Dashboard overview
- Movie management
- Showtime management
- Seat monitor
- Booking list and booking details
- Ticket verification/check-in

## MVP Limitations

- Admin authentication is intentionally simplified for the course/demo scope.
- Payment is mock payment only.
- Realtime socket updates are future scope unless enabled separately.
- The admin scope focuses on the demo booking flow instead of a full production cinema management system.

## Security Notes

- Do not commit `.env`.
- Do not expose API keys, service accounts, private keys, or production credentials.
- Keep production deployment settings separate from local demo configuration.
