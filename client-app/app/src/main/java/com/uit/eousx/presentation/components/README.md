# EOUSX Design System — Jetpack Compose

Mascot-inspired dark cinema UI kit. Drop these files into your project and start building.

---

## 📁 Files

| File | Nội dung |
|------|----------|
| `EousXTheme.kt` | Color tokens, typography, spacing, shape, MaterialTheme wrapper |
| `EousXButtons.kt` | 5 button variants |
| `EousXTextField.kt` | Text field, password field, search bar |
| `EousXStatusChip.kt` | 6 status chips + custom chip |
| `EousXMovieCard.kt` | Movie card (vertical) + Featured card (horizontal) |
| `EousXTicketCard.kt` | Booked ticket card với torn-edge divider |
| `EousXPreview.kt` | `@Preview` màn hình cho tất cả components |

---

## 🚀 Setup

### 1. Thêm dependencies vào `build.gradle.kts`

```kotlin
dependencies {
    // Compose BOM — always pin to a BOM
    implementation(platform("androidx.compose:compose-bom:2024.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coil — async image loading (dùng trong MovieCard)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ZXing — nếu muốn render QR code thật trong TicketCard
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
```

### 2. Font (tuỳ chọn)

Tải [Roboto Flex](https://fonts.google.com/specimen/Roboto+Flex) → đặt vào `res/font/` → cập nhật trong `EousXTheme.kt`:

```kotlin
val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex_regular,  FontWeight.Normal),
    Font(R.font.roboto_flex_medium,   FontWeight.Medium),
    Font(R.font.roboto_flex_semibold, FontWeight.SemiBold),
    Font(R.font.roboto_flex_bold,     FontWeight.Bold),
    Font(R.font.roboto_flex_extrabold,FontWeight.ExtraBold),
)
```

### 3. Bọc app bằng EousXTheme

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EousXTheme {
                // Your nav graph / screens here
            }
        }
    }
}
```

---

## 🧩 Component Usage

### Buttons
```kotlin
EousXPrimaryButton(text = "Book Now") { /* onClick */ }
EousXSecondaryButton(text = "View Details") { }
EousXOutlineButton(text = "Watch Trailer") { }
EousXDangerButton(text = "Cancel Booking") { }
EousXIconButton(icon = Icons.Default.FavoriteBorder) { }
```

### Inputs
```kotlin
var name by remember { mutableStateOf("") }
EousXTextField(value = name, onValueChange = { name = it }, placeholder = "Enter your name")

var pass by remember { mutableStateOf("") }
EousXPasswordField(value = pass, onValueChange = { pass = it })

var query by remember { mutableStateOf("") }
EousXSearchBar(value = query, onValueChange = { query = it }, onFilterClick = { })
```

### Status Chips
```kotlin
EousXStatusChip(status = EousXChipStatus.NOW_SHOWING)
EousXStatusChip(status = EousXChipStatus.PAID)
EousXStatusChip(status = EousXChipStatus.SOLD)

// Custom (ngoài 6 variants có sẵn)
EousXCustomChip(label = "PRE-SALE", contentColor = Color.Cyan, borderColor = Color.Cyan)
```

### Movie Card
```kotlin
EousXMovieCard(
    movie = EousXMovieData(
        id        = "dune2",
        title     = "Dune: Part Two",
        posterUrl = "https://…",
        rating    = 8.7f,
        genres    = "Sci-Fi • Adventure",
        year      = 2024,
        duration  = "2h 46m",
        status    = EousXChipStatus.NOW_SHOWING
    ),
    onBookNow   = { movie -> /* navigate to booking screen */ },
    onFavourite = { movie, isFav -> /* update wishlist */ }
)

// Hero / featured variant
EousXFeaturedMovieCard(movie = featuredMovie, onBookNow = { … })
```

### Ticket Card
```kotlin
EousXTicketCard(
    ticket = EousXTicketData(
        bookingId  = "EOUSX-240518-7A2B",
        movieTitle = "Dune: Part Two",
        date       = "Sat, 18 May 2024",
        time       = "07:30 PM",
        screen     = "IMAX 1",
        seats      = "B8, B9",
        isPaid     = true
    )
)

// Với QR code thật (dùng ZXing):
val qrBmp = BarcodeEncoder().encodeBitmap(ticket.bookingId, BarcodeFormat.QR_CODE, 200, 200)
EousXTicketCard(
    ticket  = ticket,
    qrSlot  = { Image(bitmap = qrBmp.asImageBitmap(), contentDescription = "QR") }
)
```

---

## 🎨 Color Tokens

```kotlin
EousXColors.PrimaryOrange   // #FF8A1E — CTA chính
EousXColors.NeonLime         // #C6FF00 — AI accent / NOW SHOWING
EousXColors.Charcoal         // #121418 — App background
EousXColors.SoftIvory        // #F5F2EC — Primary text
EousXColors.SlateGray        // #6B7078 — Secondary text / divider
EousXColors.DangerRed        // #E53935 — Destructive actions
```

---

## 📐 Spacing & Radius

```kotlin
// Spacing: xs=4, sm=8, md=12, lg=16, xl=20, xxl=24, xxxl=32
Modifier.padding(EousXSpacing.lg)

// Border radius: sm=8, md=12, lg=16, xl=24, pill=50
RoundedCornerShape(EousXRadius.pill)
```

---

## Cập nhật 2026-05-25

- Palette UI kit đã được chỉnh về hướng cinema dark: nền `Charcoal`, surface tối, nhấn chính vàng/cam và CTA cảnh báo đỏ cam.
- Các component dùng lại token trong `EousXTheme.kt` nhiều hơn, hạn chế màu hardcode rải rác.
- `EousXPreview.kt` có preview tổng hợp với mock data tiếng Việt cho button, text field, movie card, status chip và ticket card.
- UI kit đã được chuyển từ `presentation/files (2)` sang `presentation/components` với package `com.uit.eousx.presentation.components` để Android Studio nhận Compose Preview ổn định hơn.
- Preview tổng hợp hiện là `EousXUIKitPreview()` trong `EousXPreview.kt`, không có parameter và có đủ `@Preview`/`@Composable`.
