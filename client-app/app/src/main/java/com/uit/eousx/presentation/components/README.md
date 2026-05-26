# EousX Compose Components

Reusable Jetpack Compose UI components for the Android customer app. This folder contains app-local design tokens and shared UI pieces used by screens such as authentication, home, movie detail, checkout, ticket, and common loading/error states.

For project setup, backend rules, and course information, see the root README and `client-app/README.md`.

## Scope

These components are part of the Android module, not a standalone library. They follow the current EousX dark cinema visual style and are intended to keep screens consistent while the MVP evolves.

## Key Files

| File | Purpose |
|---|---|
| `EousXTheme.kt` | Color, typography, spacing, radius, and shared Material theme helpers. |
| `EousXButtons.kt` | Reusable primary, secondary, outline, danger, and icon button styles. |
| `EousXTextField.kt` | Text field, password field, and search field components. |
| `EousXStatusChip.kt` | Status chip styles for movie, booking, payment, ticket, and seat states. |
| `EousXMovieCard.kt` | Movie card variants used by browsing and recommendation flows. |
| `EousXTicketCard.kt` | Ticket card component with a QR content slot/placeholder. |
| `EousXPreview.kt` | Compose previews for checking component appearance during development. |
| `EousXStateViews.kt` | Shared loading, empty, and error states. |

## Dependencies

The component set relies on dependencies already declared by the Android app Gradle files, including:

- Jetpack Compose and Material 3
- Compose Material Icons Extended
- Coil Compose for remote images

QR image rendering is not wired into the current MVP ticket screen. The app currently displays the backend `qrCode` string; real QR bitmap rendering can be added later by introducing a QR library and passing rendered content into the ticket UI.

## Usage Notes

- Prefer existing tokens from `EousXColors`, `EousXTypography`, `EousXSpacing`, and `EousXRadius` before adding one-off styling.
- Keep components UI-only. Business logic, navigation, backend calls, token handling, and payment behavior should stay in screens, view models, repositories, or data layers.
- Keep backend-derived status labels aligned with Prisma/API values documented in `api-server/README.md`.

## Current MVP Notes

- The customer app uses backend movie/showtime/seat/booking data as the source of truth.
- Ticket QR image rendering is future scope.
- Seat status labels rendered by UI are `AVAILABLE`, `LOCKED`, `SOLD`, and `MAINTENANCE`.
