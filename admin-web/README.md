# EousX Admin Web

React admin dashboard for the EousX Movie Booking AI MVP. The admin web app supports the cinema/operator side of the demo: managing movies and showtimes, monitoring seats, reviewing bookings, and checking in tickets.

See the root README for course/team information.

## Stack

- React
- Vite
- TypeScript
- Tailwind CSS
- Axios
- React Router
- TanStack Query
- Lucide React

## Setup

```powershell
npm install
npm run dev
```

Build:

```powershell
npm run build
```

Preview build:

```powershell
npm run preview
```

## Environment

The admin web app talks to the EousX API server. The backend has no `/api` prefix.

Local API base URL:

```text
VITE_API_BASE_URL=http://localhost:3000
```

If `VITE_API_BASE_URL` is not set, the current API client falls back to `http://localhost:3000`.

Do not commit local `.env` files. Do not put backend secrets, Firebase service account JSON, JWT secrets, or Gemini API keys in Vite environment variables.

## Pages and Features

| Page | Route | Purpose |
|---|---|---|
| Dashboard | `/admin/dashboard` | Booking/showtime summary, paid bookings, checked-in count, latest bookings, top movies. |
| Movies | `/admin/movies` | List, create, update, and delete movies. |
| Showtimes | `/admin/showtimes` | List, create, update, and cancel showtimes. |
| Seat Monitor | `/admin/seat-monitor` | View showtime seat map and seat details. |
| Bookings | `/admin/bookings` | Review booking list and booking details. |
| Ticket Check-in | `/admin/tickets/check-in` | Verify QR code and check in valid paid tickets. |
| Rooms | `/admin/rooms` | Read-only placeholder/view for seeded rooms and seats. |

## Admin API Usage

Main endpoints used by the admin web app:

- `GET /admin/rooms`
- `GET /admin/rooms/:id`
- `GET /admin/showtimes`
- `POST /admin/showtimes`
- `PUT /admin/showtimes/:id`
- `DELETE /admin/showtimes/:id`
- `GET /admin/showtimes/:showtimeId/seats`
- `GET /admin/bookings`
- `GET /admin/bookings/:id`
- `POST /admin/movies`
- `PUT /admin/movies/:id`
- `DELETE /admin/movies/:id`
- `GET /tickets/verify/:qrCode`
- `POST /tickets/:ticketId/check-in`

The app also reads public movie/room/showtime data where needed for forms and dashboard summaries.

## Seat Monitor

The seat monitor uses TanStack Query polling when auto-refresh is enabled. The current interval in code is 5 seconds for the selected showtime.

Seat display statuses:

- `AVAILABLE`
- `LOCKED`
- `SOLD`
- `MAINTENANCE`

## MVP Scope and Limitations

- Admin authentication is omitted/simplified in the current demo scope.
- Room/seat CRUD is intentionally reduced; rooms and seats are seeded/read-only for the admin UI.
- `DELETE /admin/showtimes/:id` cancels a showtime in the backend.
- Payment is mock-only.
- Realtime WebSocket updates are future scope; the current seat monitor uses polling.

## Security Notes

- Do not commit `.env`.
- Do not expose API keys, service accounts, private keys, or production credentials.
- Keep deployment configuration separate from local demo configuration.
