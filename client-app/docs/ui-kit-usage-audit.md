# UI Kit Usage Audit

## UI kit inventory

Source reviewed first: `app/src/main/java/com/uit/eousx/presentation/components/README.md`.

Current UI kit location: `app/src/main/java/com/uit/eousx/presentation/components/`.

### Colors / theme tokens

- `EousXColors` in `EousXTheme.kt`
  - Primary/accent: `PrimaryOrange`, `NeonLime`
  - Background/surface: `Charcoal`, `Surface`, `SurfaceVariant`, `SurfaceRaised`
  - Text: `SoftIvory`, `OnSurface`, `OnSurfaceDim`, `SlateGray`, `OnAccent`
  - Semantic: `DangerRed`, `SuccessGreen`
  - Utility/alpha: `Transparent`, `OrangeAlpha12`, `OrangeAlpha20`, `DangerAlpha12`, `SuccessAlpha16`, `LimeAlpha20`, `Divider`
- `EousXSpacing`
  - `xs`, `sm`, `md`, `lg`, `xl`, `xxl`, `xxxl`
- `EousXRadius`
  - `sm`, `md`, `lg`, `xl`, `pill`
- `EousXTheme`
  - A Material 3 dark theme wrapper using the UI kit color scheme.

Note: README color comments mention older hex values for some tokens, while `EousXTheme.kt` contains the actual values currently compiled by the app. Treat Kotlin source as the source of truth.

### Typography / font

- `RobotoFlex = FontFamily.Default`
- `EousXTypography`
  - `H1`
  - `H2`
  - `Body`
  - `Caption`
  - `Label`
- Roboto Flex font files are not present yet. The UI kit is currently using Android/Compose default font fallback.

### Buttons

- `EousXPrimaryButton`
- `EousXSecondaryButton`
- `EousXOutlineButton`
- `EousXDangerButton`
- `EousXIconButton`

These are already Jetpack Compose components and can be reused immediately.

### Text fields

- `EousXTextField`
- `EousXPasswordField`
- `EousXSearchBar`

These are already Jetpack Compose components and can be reused immediately for auth, search, and form screens.

### Cards

- `EousXMovieCard`
- `EousXFeaturedMovieCard`
- `EousXTicketCard`

Cards are already Compose. `EousXMovieCard` uses Coil `AsyncImage`; `EousXTicketCard` supports a custom QR slot but does not generate real QR itself.

### Chips / badges

- `EousXStatusChip`
- `EousXCustomChip`
- `EousXChipStatus`
  - `NOW_SHOWING`
  - `UPCOMING`
  - `PAID`
  - `LOCKED`
  - `SOLD`
  - `AVAILABLE`

These can be reused for movie/showtime/payment/seat status labels.

### Bottom navigation

- Not present in the UI kit.
- The current `HomeScreen.kt` has a local `EousTabItem` and custom bottom bar using drawable icons.
- Recommendation: extract this into a reusable Compose component instead of keeping it inside `HomeScreen.kt`.

### Movie components

- Present:
  - `EousXMovieData`
  - `EousXMovieCard`
  - `EousXFeaturedMovieCard`
- Current app also has a separate `MovieCard` inside `HomeScreen.kt`.
- Recommendation: create a mapper from domain `Movie` to `EousXMovieData`, then replace the local `MovieCard` gradually.

### Seat components

- Not present.
- Needed for the future `Seat Map` screen.
- Recommended components:
  - `EousXSeat`
  - `EousXSeatLegend`
  - `EousXSeatMap`
  - `EousXScreenIndicator`
  - `SeatUiState`
  - `SeatStatus`

### Dialog / loading / empty / error states

- Not present as reusable UI kit components.
- Current screens use inline `CircularProgressIndicator`.
- Recommended components:
  - `EousXLoadingState`
  - `EousXEmptyState`
  - `EousXErrorState`
  - `EousXConfirmDialog`
  - `EousXPaymentResultDialog`

## Mapping UI kit -> Android screens

### Existing screens

| Screen | Current state | UI kit reuse |
|---|---|---|
| Splash | Uses image + Lottie, wrapped by current app theme from `ui.theme` | Keep mostly as-is. Use UI kit colors only if splash needs dark fallback/background alignment. |
| Home | Local movie card, local bottom nav, white/light layout | Replace local movie list card with `EousXMovieCard` or create `EousXMovieListItem`; extract bottom nav into `EousXBottomNavigationBar`; use `EousXSearchBar` if search is added. |
| Movie Detail | Inline poster hero, rating row, CTA button, loading state | Use `EousXPrimaryButton` for booking CTA, `EousXStatusChip` for movie status, `EousXLoadingState` once created. Keep detail hero layout custom or extract later. |

### Planned screens

| Screen | Recommended UI kit usage |
|---|---|
| Login | `EousXTextField`, `EousXPasswordField`, `EousXPrimaryButton`, `EousXSecondaryButton`; needs auth screen layout wrapper. |
| Register | Same as Login, plus form validation labels/error state support. |
| Showtime | `EousXStatusChip`, `EousXCustomChip`, `EousXPrimaryButton`; create showtime cards/date chips. |
| Seat Map | New seat components required. Existing chips can be reused for legend/status. |
| Checkout | `EousXTicketCard` as booking summary reference, `EousXPrimaryButton`, `EousXSecondaryButton`; create price summary rows. |
| Payment | `EousXPrimaryButton`, `EousXDangerButton`, status chips; create payment method cards and result dialog. |
| Ticket | `EousXTicketCard` can be reused directly; add QR generation later only if needed. |
| Booking History | `EousXTicketCard` or compact booking history card; `EousXStatusChip` for paid/locked/cancelled-like states. |

Backend note for future integration: emulator base URL should be `http://10.0.2.2:3000/`, with no `/api` prefix. This audit does not implement backend logic.

## Recommended Compose component structure

Keep the current UI kit package, but split future shared components by domain:

```text
app/src/main/java/com/uit/eousx/presentation/components/
  EousXTheme.kt
  EousXButtons.kt
  EousXTextField.kt
  EousXStatusChip.kt
  EousXMovieCard.kt
  EousXTicketCard.kt
  EousXBottomNavigation.kt
  EousXState.kt
  EousXDialog.kt
  EousXSeat.kt
  EousXShowtime.kt
  EousXCheckout.kt
```

Recommended direction:

- Keep foundation tokens centralized in `EousXTheme.kt`.
- Avoid duplicating `EousXTheme` between `com.uit.eousx.ui.theme` and `com.uit.eousx.presentation.components` long term. Pick one app-level theme entry point later.
- Keep components stateless where possible. Screens/ViewModels should own state.
- Keep backend DTOs out of UI kit components. Use small UI models such as `EousXMovieData`, `EousXTicketData`, `SeatUiState`, and mapper functions at screen/domain boundaries.
- Add previews for every reusable component. Current UI kit already has individual previews for the main components and one aggregate `EousXUIKitPreview`.

## Files to create

No backend files should be created for the next UI-only step.

Recommended UI files:

- `app/src/main/java/com/uit/eousx/presentation/components/EousXBottomNavigation.kt`
  - Extract current `EousTabItem` and bottom bar behavior.
- `app/src/main/java/com/uit/eousx/presentation/components/EousXState.kt`
  - Loading, empty, and error UI.
- `app/src/main/java/com/uit/eousx/presentation/components/EousXDialog.kt`
  - Confirm/result dialogs.
- `app/src/main/java/com/uit/eousx/presentation/components/EousXSeat.kt`
  - Seat map primitives and legend.
- `app/src/main/java/com/uit/eousx/presentation/components/EousXShowtime.kt`
  - Date chip, showtime chip/card.
- `app/src/main/java/com/uit/eousx/presentation/components/EousXCheckout.kt`
  - Price rows, checkout summary, payment method card.
- `app/src/main/java/com/uit/eousx/presentation/auth/LoginScreen.kt`
- `app/src/main/java/com/uit/eousx/presentation/auth/RegisterScreen.kt`
- `app/src/main/java/com/uit/eousx/presentation/booking/ShowtimeScreen.kt`
- `app/src/main/java/com/uit/eousx/presentation/booking/SeatMapScreen.kt`
- `app/src/main/java/com/uit/eousx/presentation/booking/CheckoutScreen.kt`
- `app/src/main/java/com/uit/eousx/presentation/payment/PaymentScreen.kt`
- `app/src/main/java/com/uit/eousx/presentation/ticket/TicketScreen.kt`
- `app/src/main/java/com/uit/eousx/presentation/ticket/BookingHistoryScreen.kt`

## Files to modify

Recommended staged modifications:

- `app/src/main/java/com/uit/eousx/MainActivity.kt`
  - Add routes for Login, Register, Showtime, Seat Map, Checkout, Payment, Ticket, Booking History after screen skeletons exist.
  - Keep backend wiring out of this step.
- `app/src/main/java/com/uit/eousx/presentation/Screen.kt`
  - Add sealed routes for planned screens.
- `app/src/main/java/com/uit/eousx/presentation/HomeScreen.kt`
  - Replace inline bottom bar with `EousXBottomNavigation`.
  - Replace local movie card usage gradually with UI kit movie component or a compact app-specific list item.
  - Avoid changing data fetching logic.
- `app/src/main/java/com/uit/eousx/presentation/MovieDetailScreen.kt`
  - Replace inline booking `Button` with `EousXPrimaryButton`.
  - Add status chips or booking CTA styling from UI kit.
- `app/src/main/java/com/uit/eousx/ui/theme/Theme.kt`
  - Later decision needed: merge existing app theme with UI kit theme or make the UI kit theme the app-level theme.
- `app/src/main/java/com/uit/eousx/di/NetworkModule.kt`
  - Future backend integration only: change base URL to `http://10.0.2.2:3000/` and remove TMDB-specific auth header for app backend clients. Do not do this in the UI-only step.

## Risks

- Theme duplication: there are two `EousXTheme` functions in different packages:
  - `com.uit.eousx.ui.theme.EousXTheme`
  - `com.uit.eousx.presentation.components.EousXTheme`
  This can cause inconsistent colors and confusing imports.
- Current Home and Movie Detail screens are mostly light-theme custom UI, while the UI kit is dark cinema style. Mixing both without a staged migration will look inconsistent.
- README text is mojibake in several places and color comments are not fully aligned with current Kotlin token values. Kotlin source should be treated as authoritative.
- `EousXMovieCard` expects `EousXMovieData`, while current app uses domain `Movie`. A mapper is needed to avoid leaking domain/API details into UI components.
- Seat map, dialogs, loading/empty/error states, showtime components, payment method cards, and bottom navigation are not in the UI kit yet.
- `EousXTicketCard` has a QR slot but no QR generation dependency or implementation yet.
- Current `NetworkModule` points to TMDB (`https://api.themoviedb.org/3/`) and injects TMDB bearer auth. Future backend integration must separate this from EousX backend base URL `http://10.0.2.2:3000/`.
- There is a stale-looking `presentation/files (2)` path in git status. Keep using `presentation/components` as the active UI kit path.

## Next implementation prompt

```text
Using docs/ui-kit-usage-audit.md as the source of truth, implement the next UI-only step.

Scope:
1. Do not implement backend logic.
2. Do not rewrite the whole app.
3. Keep existing Splash, Home, and Movie Detail behavior working.
4. Create reusable Compose components:
   - EousXBottomNavigation.kt
   - EousXState.kt
   - EousXSeat.kt
   - EousXShowtime.kt
   - EousXCheckout.kt
5. Add previews for each new component.
6. Add screen skeletons only:
   - LoginScreen
   - RegisterScreen
   - ShowtimeScreen
   - SeatMapScreen
   - CheckoutScreen
   - PaymentScreen
   - TicketScreen
   - BookingHistoryScreen
7. Update Screen.kt and MainActivity.kt routes only enough for navigation skeletons.
8. Use the UI kit components from presentation/components wherever possible.
9. Verify with ./gradlew.bat :app:compileDebugKotlin.
```
