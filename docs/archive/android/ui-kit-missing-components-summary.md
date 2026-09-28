# UI Kit Missing Components Summary

## Files created

- `app/src/main/java/com/uit/eousx/presentation/components/EousXBottomNavigation.kt`
- `app/src/main/java/com/uit/eousx/presentation/components/EousXState.kt`
- `app/src/main/java/com/uit/eousx/presentation/components/EousXDialog.kt`
- `app/src/main/java/com/uit/eousx/presentation/components/EousXSeat.kt`
- `app/src/main/java/com/uit/eousx/presentation/components/EousXShowtime.kt`
- `app/src/main/java/com/uit/eousx/presentation/components/EousXCheckout.kt`

## Components created

- Bottom navigation
  - `EousXBottomNavItem`
  - `EousXBottomNavigationBar`
- State views
  - `EousXLoadingState`
  - `EousXEmptyState`
  - `EousXErrorState`
- Dialogs
  - `EousXConfirmDialog`
  - `EousXResultDialog`
- Seats
  - `SeatStatus`
  - `SeatUiState`
  - `EousXSeat`
  - `EousXSeatLegend`
  - `EousXScreenIndicator`
- Showtimes
  - `ShowtimeUiState`
  - `EousXDateChip`
  - `EousXShowtimeCard`
- Checkout/payment
  - `EousXPriceRow`
  - `EousXCheckoutSummaryCard`
  - `PaymentProviderUi`
  - `EousXPaymentMethodCard`

## How to use each component

### Bottom navigation

```kotlin
EousXBottomNavigationBar(
    items = listOf(
        EousXBottomNavItem("home", "Home", Icons.Default.Home),
        EousXBottomNavItem("tickets", "Tickets", Icons.Default.ConfirmationNumber)
    ),
    selectedItemId = "home",
    onItemSelected = { item -> }
)
```

### State views

```kotlin
EousXLoadingState(message = "Loading")
EousXEmptyState(title = "No items", message = "There is nothing to show yet.")
EousXErrorState(title = "Something went wrong", onRetryClick = {})
```

### Dialogs

```kotlin
EousXConfirmDialog(
    title = "Confirm action",
    message = "This action needs your confirmation.",
    confirmText = "Confirm",
    dismissText = "Cancel",
    onConfirm = {},
    onDismiss = {}
)

EousXResultDialog(
    title = "Completed",
    message = "The action finished successfully.",
    confirmText = "Done",
    onConfirm = {}
)
```

### Seats

```kotlin
EousXScreenIndicator()
EousXSeat(
    seat = SeatUiState(id = "A1", label = "A1", status = SeatStatus.AVAILABLE),
    onClick = { seat -> }
)
EousXSeatLegend()
```

### Showtimes

```kotlin
EousXDateChip(label = "Mon", supportingText = "25", selected = true, onClick = {})

EousXShowtimeCard(
    showtime = ShowtimeUiState(
        id = "st-1",
        time = "19:30",
        roomName = "Room 01",
        roomType = "IMAX",
        basePrice = "$12.00",
        availableSeats = 48
    ),
    selected = true,
    onClick = { showtime -> }
)
```

### Checkout/payment

```kotlin
EousXCheckoutSummaryCard(
    rows = listOf("Tickets" to "$24.00", "Service fee" to "$1.20"),
    totalLabel = "Total",
    totalValue = "$25.20"
)

EousXPaymentMethodCard(
    provider = PaymentProviderUi.MOCK,
    selected = true,
    onClick = { provider -> }
)
```

## Preview list

- `EousXBottomNavigationPreview`
- `EousXStatesPreview`
- `EousXConfirmDialogPreview`
- `EousXResultDialogPreview`
- `EousXSeatsPreview`
- `EousXShowtimePreview`
- `EousXCheckoutPreview`

## Build result

Command run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Result: `BUILD SUCCESSFUL`.

## Any warnings/limitations

- Gradle emitted the existing KSP warning about generated Hilt sources not reporting dependencies.
- Gradle emitted existing deprecation warnings related to future Gradle 10 compatibility.
- No backend DTOs, navigation, `MainActivity.kt`, app-level theme, Home, or Movie Detail files were changed.
- Dialog previews are separate because Compose `AlertDialog` is a full overlay component.
- `EousXTicketCard` still only exposes a QR slot; no QR generation dependency was added.
