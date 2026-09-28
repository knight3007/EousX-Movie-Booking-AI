# Android Client Design Audit

Phạm vi audit: `ui/theme/*`, `HomeScreen.kt`, `MovieDetailScreen.kt`, `SplashScreen.kt`, resource theme XML và các drawable/raw liên quan. Không implement thay đổi design trong lần này.

## 1. Current Color System

### Compose color tokens trong `Color.kt`

| Token/Variable | HEX | Used for | Notes |
|---|---:|---|---|
| `EousOrange` | `#E83D21` | Primary/logo accent, loading spinner, "ĐẶT VÉ NGAY" button | Màu cam/đỏ cam hiện là màu hành động chính. |
| `EousYellowSelected` | `#EFCA6C` | Selected bottom tab, star/rating icon, theme secondary | Vàng nhấn hiện tại. |
| `EousYellowLight` | `#F8F1DE` | Bottom navigation background, XML splash/window background | Vàng nhạt/nền brand hiện tại. |
| `EousGreyText` | `#666666` | Theme tertiary | Token có khai báo nhưng ít thấy dùng trực tiếp trong screen. |

### XML colors trong `res/values/colors.xml`

| Token/Variable | HEX | Used for | Notes |
|---|---:|---|---|
| `purple_200` | `#FFBB86FC` | Không thấy dùng trong Compose hiện tại | Default template leftover. |
| `purple_500` | `#FF6200EE` | Không thấy dùng trong Compose hiện tại | Default template leftover. |
| `purple_700` | `#FF3700B3` | Không thấy dùng trong Compose hiện tại | Default template leftover. |
| `teal_200` | `#FF03DAC5` | Không thấy dùng trong Compose hiện tại | Default template leftover. |
| `teal_700` | `#FF018786` | Không thấy dùng trong Compose hiện tại | Default template leftover. |
| `black` | `#FF000000` | XML/default resource | Standard black. |
| `white` | `#FFFFFFFF` | XML/default resource | Standard white. |

### Hardcoded colors tìm thấy

| File | Color | Usage |
|---|---:|---|
| `Theme.kt` | `#111111` | Dark `background`, dark `surface`. |
| `Theme.kt` | `Color.White` | Light `background`, `surface`, `onPrimary`. |
| `Theme.kt` | `Color.Black` | Light `onBackground`, `onSurface`. |
| `HomeScreen.kt` | `Color.White` | Scaffold background, bottom bar border. |
| `HomeScreen.kt` | `Color.Transparent` | FAB background, unselected tab background. |
| `HomeScreen.kt` | `#F9F9F9` | Movie card container. |
| `HomeScreen.kt` | `Color.LightGray` | Poster placeholder background. |
| `HomeScreen.kt` | `Color.Gray` | Release date text. |
| `MovieDetailScreen.kt` | `Color.Transparent` | Poster gradient start. |
| `MovieDetailScreen.kt` | `Color.White` | Poster gradient end, back icon tint. |
| `MovieDetailScreen.kt` | `Color.Black.copy(alpha = 0.4f)` | Back button circular background. |
| `MovieDetailScreen.kt` | `Color.Gray` | Release date text. |
| `MovieDetailScreen.kt` | `Color.DarkGray` | Overview body text. |
| `res/values/themes.xml` | `#F8F1DE` | Status bar, window background, splash background. |

## 2. Current Theme Mapping

`Theme.kt` có cả `darkColorScheme` và `lightColorScheme`.

`EousXTheme`:

- `darkTheme: Boolean = isSystemInDarkTheme()`
- `dynamicColor: Boolean = false`
- Nếu `dynamicColor = true` và Android S+ thì dùng dynamic scheme, nhưng default hiện tại là tắt.
- Nếu system dark mode -> dùng `DarkColorScheme`.
- Nếu system light mode -> dùng `LightColorScheme`.

### DarkColorScheme

| Material role | Value |
|---|---|
| `primary` | `EousOrange` `#E83D21` |
| `secondary` | `EousYellowSelected` `#EFCA6C` |
| `tertiary` | `EousGreyText` `#666666` |
| `background` | `#111111` |
| `surface` | `#111111` |
| `onPrimary` | Material default, not explicitly set |
| `onBackground` | Material default, not explicitly set |
| `error` | Material default, not explicitly set |

### LightColorScheme

| Material role | Value |
|---|---|
| `primary` | `EousOrange` `#E83D21` |
| `secondary` | `EousYellowSelected` `#EFCA6C` |
| `tertiary` | `EousGreyText` `#666666` |
| `background` | `Color.White` |
| `surface` | `Color.White` |
| `onPrimary` | `Color.White` |
| `onBackground` | `Color.Black` |
| `onSurface` | `Color.Black` |
| `error` | Material default, not explicitly set |

App hiện không ưu tiên dark hay light một cách rõ ràng; nó theo system setting. Tuy nhiên screen hiện tại hardcode nhiều màu light (`Color.White`, `Color.Gray`, `Color.DarkGray`, `#F9F9F9`), nên thực tế UI đang thiên về light mode và có thể không đẹp trong system dark mode.

Home/Movie Detail/Splash có hardcode màu ngoài theme:

- Home: có nhiều hardcode light colors.
- Movie Detail: có hardcode gradient white, black overlay, gray/dark gray.
- Splash: dùng image background, không dùng `MaterialTheme.colorScheme`.
- XML splash/window theme hardcode `#F8F1DE`.

## 3. Current Typography System

`Type.kt` chỉ override `bodyLarge`. Các style khác đang dùng default Material 3 typography.

Không thấy `res/font`. Không có custom font file. Font hiện tại là system/default font qua `FontFamily.Default`.

| Text Style | Font Family | Size | Weight | Line Height | Letter Spacing | Used for |
|---|---|---:|---|---:|---:|---|
| `bodyLarge` | `FontFamily.Default` | `16.sp` | `Normal` | `24.sp` | `0.5.sp` | Overview text trong Movie Detail dùng `MaterialTheme.typography.bodyLarge`, nhưng override thêm `lineHeight = 26.sp`. |
| `displayLarge` | Material default | Material default | Material default | Material default | Material default | Không thấy dùng trực tiếp. |
| `headlineLarge` | Material default | Material default | Material default | Material default | Material default | Không thấy dùng trực tiếp. |
| `headlineMedium` | Material default | Material default | Material default + override `Bold` ở call site | Material default | Material default | Movie Detail title. |
| `titleLarge` | Material default | Material default | Material default + override `Bold` ở call site | Material default | Material default | Section title "Nội dung". |
| `titleMedium` | Material default | Material default | Material default + override `Bold` ở call site | Material default | Material default | Movie card title. |
| `bodyMedium` | Material default | Material default | Material default + override `Bold` ở call site | Material default | Material default | Rating value in movie card. |
| `bodySmall` | Material default | Material default | Material default | Material default | Material default | Release date in movie card. |
| `labelLarge` | Material default | Material default | Material default | Material default | Material default | Button text does not explicitly use `labelLarge`; it hardcodes `fontSize = 18.sp`, `Bold`. |

## 4. Current UI Style Usage

### HomeScreen

- Màu nền: `Scaffold(containerColor = Color.White)`, hardcode light background.
- Text: phần lớn dùng `MaterialTheme.typography` (`titleMedium`, `bodySmall`, `bodyMedium`) nhưng override `FontWeight.Bold` ở call site.
- Loading spinner: `CircularProgressIndicator(color = EousOrange)`.
- Bottom bar:
  - Background `EousYellowLight`.
  - Border `2.dp` `Color.White`.
  - Shape `RoundedCornerShape(35.dp)`.
  - Shadow elevation `8.dp`.
  - Selected tab background `EousYellowSelected`.
- Movie card:
  - `height(140.dp)`.
  - Shape `RoundedCornerShape(16.dp)`.
  - Container `Color(0xFFF9F9F9)`.
  - Elevation `4.dp`.
  - Poster width `100.dp`, clipped top/bottom start 16dp.
  - Poster placeholder `Color.LightGray`.
- Hardcoded colors: `Color.White`, `Color.Transparent`, `#F9F9F9`, `Color.LightGray`, `Color.Gray`.
- Style lặp lại nên tách token/component:
  - Card shape/elevation/background.
  - Bottom nav shape/elevation/background.
  - Poster base URL handling nên nằm ở data/UI mapper, không lặp nối URL trong UI.

### MovieDetailScreen

- Màu nền: `Scaffold` không set `containerColor`, mặc định theo theme surface/background, nhưng nội dung hardcode gradient về `Color.White`.
- Text:
  - Title dùng `MaterialTheme.typography.headlineMedium` + `Bold`.
  - Section title dùng `titleLarge` + `Bold`.
  - Overview dùng `bodyLarge`, hardcode `lineHeight = 26.sp`, `Color.DarkGray`.
  - Button text hardcode `fontSize = 18.sp`, `Bold`.
- Button "Đặt vé":
  - `ButtonDefaults.buttonColors(containerColor = EousOrange)`.
  - Shape `RoundedCornerShape(16.dp)`.
  - Height `56.dp`.
  - Text: "ĐẶT VÉ NGAY".
- Poster area:
  - Height `350.dp`.
  - Image crop full area.
  - Vertical gradient `Color.Transparent -> Color.White`.
  - Back button circular overlay `Color.Black.copy(alpha = 0.4f)`.
- Hardcoded colors: `Color.White`, `Color.Black.copy(alpha = 0.4f)`, `Color.Gray`, `Color.DarkGray`, `Color.Transparent`.

### SplashScreen

- Màu nền: dùng bitmap `bg_splash.png`, không dùng theme background.
- Lottie:
  - Size `220.dp`.
  - Align center, offset `x = 30.dp`, `y = 200.dp`.
- Điều hướng:
  - Khi animation progress `1f` -> Home.
- Hardcoded visual values: size/offset. Không có color hardcode trong composable, nhưng XML theme hardcode `#F8F1DE`.

## 5. Current Design Strengths

Điểm nên giữ:

- Splash có identity riêng nhờ ảnh nền + Lottie animation.
- Home card layout đơn giản, dễ quét, poster/title/release/rating đủ cho bước browse phim.
- Bottom navigation có hình ảnh brand/icon riêng, tạo cảm giác app custom hơn default Material.
- Movie Detail poster hero + gradient tạo chuyển tiếp thị giác tốt.
- CTA "ĐẶT VÉ NGAY" rõ, dễ thấy, đặt ở bottom bar đúng workflow mobile.
- Màu `EousOrange` và vàng hiện tại có nhận diện brand riêng, phù hợp app phim/vé.
- Code đã dùng Compose preview cho Home/MovieCard/MovieDetail/Splash, hữu ích khi refactor UI sau này.

## 6. Current Design Problems

Điểm cần tối ưu sau:

- Màu chưa đồng bộ qua theme; nhiều hardcoded colors trong screen.
- Dark/light mode chưa rõ. Theme có dark scheme nhưng screens hardcode light colors nên dark mode dễ lệch.
- Typography chưa có nhận diện riêng; mới override `bodyLarge`, còn lại dùng Material default.
- Font chưa có custom brand font.
- Button/card/bottom nav chưa gom thành token/component trung tâm.
- Spacing chưa có design token (`Dimens.kt` chưa có).
- Shape chưa có token (`Shape.kt` chưa có).
- XML colors vẫn còn default purple/teal template, dễ gây nhiễu.
- `EousGreyText` khai báo nhưng chưa được tận dụng rõ.
- Poster URL đang có nguy cơ xử lý không nhất quán: repository đã nối full TMDB image URL, nhưng UI lại nối tiếp `"https://image.tmdb.org/t/p/w500${movie.posterPath}"` ở Home/Detail. Điều này là vấn đề data/UI hơn là design, nhưng ảnh vỡ sẽ ảnh hưởng trực tiếp đến UI.

## 7. Future EousX Mobile Design Direction

Admin Web hiện dùng:

- English UI only.
- Dark ZZZ-like theme.
- Heading font: Barlow Condensed.
- Body font: Inter.
- Dark background.
- Yellow highlight.
- Red warning/accent.
- Bold uppercase title.
- Card-based layout.

Nên đồng bộ mobile app theo hướng này sau khi REST booking flow ổn. Lý do: brand nhất quán giữa Admin Web và Android, hợp domain cinema, và dark UI phù hợp trải nghiệm chọn phim/vé. Tuy nhiên không nên đổi toàn bộ theme cùng lúc với backend integration phase đầu, vì sẽ tăng blast radius và khó debug.

Android equivalent tokens đề xuất:

```kotlin
object EousXColors {
    val Background = Color(0xFF0B0B0F)
    val Panel = Color(0xFF15151C)
    val Card = Color(0xFF1E1E28)

    val Primary = Color(0xFFF5C400)
    val PrimaryDark = Color(0xFFD9A900)

    val Danger = Color(0xFFFF3B3B)
    val Success = Color(0xFF32D583)

    val TextPrimary = Color(0xFFF5F5F5)
    val TextMuted = Color(0xFF9CA3AF)
    val Border = Color(0xFF2D2D38)
}
```

Material mapping gợi ý:

| Material role | Proposed token |
|---|---|
| `primary` | `EousXColors.Primary` |
| `onPrimary` | `EousXColors.Background` |
| `secondary` | `EousXColors.Danger` |
| `background` | `EousXColors.Background` |
| `surface` | `EousXColors.Panel` |
| `surfaceVariant` | `EousXColors.Card` |
| `onBackground` | `EousXColors.TextPrimary` |
| `onSurface` | `EousXColors.TextPrimary` |
| `outline` | `EousXColors.Border` |
| `error` | `EousXColors.Danger` |

Component direction:

- Home background dark.
- Movie cards use `Card` with subtle border `Border`.
- CTA primary uses yellow `Primary`, text dark.
- Danger/cancel/payment failure uses red `Danger`.
- Success/paid/ticket valid uses green `Success`.
- Movie title uppercase/bold for hero/detail.
- Keep cards radius around 8-12dp for app surfaces; avoid very rounded nested cards except bottom nav/FAB if brand requires.

## 8. Font Recommendation for Android

Không thấy font file trong `res/font`. Hiện project dùng system/default font.

Đề xuất sau này, chưa implement vội:

- Heading/logo/big title: Barlow Condensed hoặc Oswald.
- Body/form/list: Inter hoặc Roboto fallback.

Ghi chú:

- Android có thể dùng `res/font` nếu thêm font file sau này.
- Không tự thêm font file nếu chưa được yêu cầu.
- Không commit font/license lạ nếu chưa kiểm tra license.
- Trước mắt có thể giữ system font để tránh lỗi build, chỉ document plan.

Typography direction đề xuất:

```kotlin
val EousXTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
)
```

## 9. Output cuối cùng

### Bảng mã màu hiện tại

| Token/Variable | HEX | Source |
|---|---:|---|
| `EousOrange` | `#E83D21` | `Color.kt` |
| `EousYellowSelected` | `#EFCA6C` | `Color.kt` |
| `EousYellowLight` | `#F8F1DE` | `Color.kt`, `themes.xml` |
| `EousGreyText` | `#666666` | `Color.kt` |
| Dark background/surface | `#111111` | `Theme.kt` |
| Movie card background | `#F9F9F9` | `HomeScreen.kt` |
| XML template purple/teal | `#FFBB86FC`, `#FF6200EE`, `#FF3700B3`, `#FF03DAC5`, `#FF018786` | `colors.xml`, likely unused leftovers |

### Bảng typography hiện tại

| Text Style | Font Family | Size | Weight | Line Height | Letter Spacing | Used for |
|---|---|---:|---|---:|---:|---|
| `bodyLarge` | Default | `16.sp` | Normal | `24.sp` | `0.5.sp` | Body text baseline; Movie Detail overview uses it with lineHeight override. |
| Other Material styles | Material default | Default | Default | Default | Default | Used by Home/Detail, mostly with local `FontWeight.Bold` overrides. |
| Button text | Default | `18.sp` | Bold | Not set | Not set | "ĐẶT VÉ NGAY", hardcoded in Movie Detail. |

### Danh sách màu hardcode tìm thấy

- `Color.White`
- `Color.Black`
- `Color.Black.copy(alpha = 0.4f)`
- `Color.Gray`
- `Color.DarkGray`
- `Color.LightGray`
- `Color.Transparent`
- `Color(0xFFF9F9F9)`
- `Color(0xFF111111)`
- XML `#F8F1DE`
- XML default template purple/teal colors.

### Danh sách màn hình đang dùng theme tốt

- `MovieDetailScreen.kt`: có dùng `MaterialTheme.typography` cho title/section/body, nhưng màu vẫn hardcode nhiều.
- `HomeScreen.kt`: có dùng `MaterialTheme.typography` cho movie card text, nhưng màu/layout token chưa tập trung.
- `SplashScreen.kt`: preview bọc `EousXTheme`, nhưng visual chính là image/Lottie chứ không dựa vào color scheme.

### Danh sách màn hình cần refactor style

- `HomeScreen.kt`: cần thay `Color.White`, `#F9F9F9`, `Color.Gray`, `Color.LightGray` bằng tokens; tách movie card/bottom nav styles.
- `MovieDetailScreen.kt`: cần thay gradient/light text colors bằng tokens; chuẩn hóa CTA button style; đảm bảo dark mode.
- `SplashScreen.kt`: cần xem lại hardcoded Lottie offset/size theo responsive constraints nếu đổi brand/theme.
- `Theme.kt`: cần map đầy đủ dark theme, error, outline, surface variants.
- `Type.kt`: cần define typography system rõ hơn khi có brand fonts.

### Đề xuất design token Android tương lai

```kotlin
object EousXColors {
    val Background = Color(0xFF0B0B0F)
    val Panel = Color(0xFF15151C)
    val Card = Color(0xFF1E1E28)
    val Primary = Color(0xFFF5C400)
    val PrimaryDark = Color(0xFFD9A900)
    val Danger = Color(0xFFFF3B3B)
    val Success = Color(0xFF32D583)
    val TextPrimary = Color(0xFFF5F5F5)
    val TextMuted = Color(0xFF9CA3AF)
    val Border = Color(0xFF2D2D38)
}
```

### Có nên đồng bộ mobile app với Admin Web ZZZ-like theme không?

Có, nhưng nên làm sau khi backend REST booking flow chạy ổn.

Nên đồng bộ vì:

- Giữ brand EousX nhất quán giữa Admin Web và Android.
- Dark cinema theme hợp ngữ cảnh movie booking hơn light default UI.
- Yellow highlight + red accent tạo hierarchy rõ cho CTA, warning, payment/cancel states.
- Heading condensed uppercase hợp poster/movie UI.

Không nên làm ngay trong phase đầu vì:

- Backend integration đã có nhiều rủi ro về auth/token/id/showtime/seat lock.
- Đổi theme lớn cùng lúc dễ làm khó debug UI và logic.
- UI hiện tại đang chạy light mode tương đối ổn; nên giữ trong Phase 0-2, sau đó refactor token có kiểm soát.
