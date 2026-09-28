# Android Client Current Audit

Phạm vi audit: Android client trong root project `client-app` tại `app/src/main/java/com/uit/eousx`. Không có thư mục con `android-client`; cấu trúc hiện tại là Gradle Android app module `:app`.

## 1. App hiện đang có những màn hình nào?

Các màn hình/composable hiện có:

| Screen | File | Trạng thái |
|---|---|---|
| Splash | `app/src/main/java/com/uit/eousx/presentation/SplashScreen.kt` | Có thật, dùng splash native + ảnh nền `bg_splash.png` + Lottie `movie_cut.json`, tự điều hướng sang Home khi animation xong. |
| Home | `app/src/main/java/com/uit/eousx/presentation/HomeScreen.kt` | Có thật, hiển thị danh sách phim từ `MovieViewModel`, bottom tab 3 mục, FAB Chat AI placeholder. |
| Movie Detail | `app/src/main/java/com/uit/eousx/presentation/MovieDetailScreen.kt` | Có thật, lấy movie theo `movieId`, hiển thị poster, title, rating, release date, overview, nút đặt vé. |
| Ticket tab placeholder | `HomeScreen.kt` | Chỉ là placeholder text trong tab 1: "Màn hình ĐẶT VÉ". |
| Profile tab placeholder | `HomeScreen.kt` | Chỉ là placeholder text trong tab 2: "Màn hình CÁ NHÂN". |
| Chat AI | `Screen.kt` | Chỉ khai báo route, chưa có composable trong `NavHost`. |

Chưa có các màn hình trong booking flow: Login, Register, Showtime Selection, Seat Map, Checkout, Payment, Ticket QR, Booking History.

## 2. Navigation hiện tại đang dùng route nào?

Navigation nằm trong `MainActivity.kt`, dùng `NavHost` của Navigation Compose:

| Route | Khai báo | Có đăng ký trong NavHost? | Ghi chú |
|---|---|---:|---|
| `splash_screen` | `Screen.Splash` | Có | Start destination. |
| `home_screen` | `Screen.Home` | Có | Splash điều hướng sang Home. |
| `detail_screen/{movieId}` | `Screen.Detail` | Có | Argument `movieId` kiểu `NavType.IntType`. |
| `chat_ai_screen` | `Screen.ChatAI` | Không | Mới khai báo để dùng sau. |

Home điều hướng sang detail bằng `Screen.Detail.createRoute(movieId)`, trong đó `movieId` là `Int`.

## 3. App hiện đang lấy phim từ đâu? TMDB hay backend?

App hiện lấy phim từ TMDB, không lấy từ backend EousX.

Các dấu hiệu chính:

- `NetworkModule.kt` cấu hình Retrofit base URL là `https://api.themoviedb.org/3/`.
- `TmdbApi.kt` gọi `GET movie/popular` và `GET movie/now_playing`.
- `MovieRepositoryImpl.kt` nhận `TmdbApi`, map `MovieDto` sang domain `Movie`.
- API key TMDB đọc từ `local.properties` key `TMDB_API_KEY`, inject vào `BuildConfig.TMDB_API_KEY`.

Hiện chưa có `BackendApi.kt` hoặc Retrofit client cho `http://10.0.2.2:3000/`.

## 4. Model Movie hiện tại dùng id kiểu gì?

`domain/model/Movie.kt`:

```kotlin
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String,
    val voteAverage: Double
)
```

`MovieDto.id` trong `MovieResponse.kt` cũng là `Int`, tương ứng TMDB movie id.

Nguy cơ mismatch:

- Android hiện coi `movieId` là `Int`.
- Backend EousX nhiều khả năng dùng `String`/UUID cho `Movie.id` và các endpoint dạng `/movies/:movieId/showtimes`.
- Nếu chuyển thẳng Home/Movie Detail sang backend, navigation detail hiện tại phải đổi argument từ `Int` sang `String`.
- Nếu giữ TMDB cho Home, booking sẽ cần mapping chắc chắn từ `tmdbId: Int` sang backend `movieId: String`. Nếu backend chưa có field `tmdbId`, flow booking sẽ không ổn định.

## 5. NetworkModule hiện tại đang cấu hình base URL nào?

`di/NetworkModule.kt`:

```kotlin
.baseUrl("https://api.themoviedb.org/3/")
```

Đây là TMDB base URL. Chưa có base URL backend EousX.

Lưu ý khi tích hợp backend Android emulator:

- Dùng `http://10.0.2.2:3000/`.
- Không dùng `http://localhost:3000/` trong emulator.
- Backend hiện tại không dùng `/api` prefix.

## 6. Retrofit/Moshi/OkHttp/Hilt đã setup ra sao?

Đã setup ở mức cơ bản:

- Hilt:
  - `EousXApplication.kt` có `@HiltAndroidApp`.
  - `MainActivity.kt` có `@AndroidEntryPoint`.
  - `MovieViewModel` và `MovieDetailViewModel` có `@HiltViewModel`.
  - `RepositoryModule.kt` bind `MovieRepositoryImpl` vào `MovieRepository`.
- Retrofit:
  - Một singleton Retrofit trong `NetworkModule`.
  - Chỉ phục vụ TMDB hiện tại.
- Moshi:
  - `Moshi.Builder().add(KotlinJsonAdapterFactory()).build()`.
  - DTO dùng `@JsonClass(generateAdapter = true)`.
- OkHttp:
  - Có `HttpLoggingInterceptor` level `BODY`.
  - Có interceptor gắn `Authorization: Bearer ${BuildConfig.TMDB_API_KEY}` và `accept: application/json`.
  - Interceptor này dành cho TMDB, không dùng được trực tiếp cho backend EousX JWT.

Rủi ro hiện tại: nếu mở rộng `NetworkModule` thiếu qualifier, hai Retrofit/OkHttp clients có thể conflict trong Hilt. Nên dùng `@Named` hoặc custom qualifier cho TMDB và Backend.

## 7. Firebase Auth đã được dùng thật chưa hay mới có dependency?

Firebase mới có dependency, chưa dùng thật trong source hiện tại.

Trong `app/build.gradle.kts` có:

- Firebase BOM
- Firebase Analytics
- Firebase Auth
- Firebase Firestore
- Firebase Messaging
- Firebase AI

Nhưng audit source không thấy import/sử dụng `FirebaseAuth`, Firestore, Messaging hoặc Firebase AI trong Kotlin code. Root `build.gradle.kts` có khai báo plugin `google-services`, nhưng `app/build.gradle.kts` chưa apply plugin này. Không thấy `google-services.json` ở:

- `google-services.json`
- `app/google-services.json`
- `app/src/main/google-services.json`

Không in nội dung secret vì file không tồn tại.

## 8. Room đã dùng thật chưa hay mới setup dependency?

Room mới có dependency, chưa dùng thật.

Trong `app/build.gradle.kts` có:

- `room-runtime`
- `room-ktx`
- `room-compiler` qua KSP

Nhưng source hiện không có `@Entity`, `@Dao`, `RoomDatabase`, database class hoặc repository local cache.

## 9. Nút "Đặt vé" hiện tại đang làm gì?

Trong `MovieDetailScreen.kt`, nút gọi callback:

```kotlin
onBookClick = {}
```

Nghĩa là hiện tại nút "ĐẶT VÉ NGAY" không làm gì. Chưa điều hướng sang Showtime Selection, chưa gọi API backend, chưa yêu cầu login.

## 10. Những phần nào có thể tái sử dụng để nối backend?

Có thể tái sử dụng:

- Compose UI hiện tại của Home và Movie Detail, đặc biệt layout list card, poster, rating, overview.
- Navigation Compose setup trong `MainActivity.kt` và sealed `Screen`.
- Hilt setup: `EousXApplication`, `@AndroidEntryPoint`, `@HiltViewModel`, module pattern.
- Retrofit/Moshi/OkHttp pattern trong `NetworkModule`.
- Repository pattern: domain repository interface + data implementation.
- Loading state bằng `StateFlow` trong ViewModel.
- Coil `AsyncImage` cho poster.
- Lottie splash hiện tại.
- Bottom navigation visual nếu muốn giữ branding hiện tại.

## 11. Những phần nào không nên đụng mạnh để tránh phá UI hiện tại?

Không nên rewrite mạnh:

- `HomeScreen.kt`: giữ layout list/card/bottom bar, chỉ đổi data source hoặc mapper theo phase.
- `MovieDetailScreen.kt`: giữ layout poster/detail/button, chỉ nối nút đặt vé sang Showtime Selection.
- `SplashScreen.kt`: giữ animation và điều hướng, sau này chỉ thêm logic kiểm tra token nếu cần.
- `ui/theme/*`: chưa nên thay theme toàn diện trong phase backend integration đầu tiên.
- `MainActivity.kt`: mở rộng route dần, không thay toàn bộ navigation architecture.
- `MovieRepository` hiện tại nếu vẫn cần TMDB song song; nên thêm backend repository riêng hoặc refactor có kiểm soát.

## File Android liên quan hiện có

| File | Vai trò |
|---|---|
| `settings.gradle.kts` | Root project `EousX`, include `:app`. |
| `build.gradle.kts` | Plugin aliases root, có khai báo Google services plugin nhưng chưa apply ở app. |
| `gradle/libs.versions.toml` | Version catalog cho Compose, Hilt, Firebase, Retrofit, Room, Coil, Media3. |
| `app/build.gradle.kts` | Android app config, dependencies, TMDB API key BuildConfig. |
| `app/src/main/AndroidManifest.xml` | Internet permission, application `.EousXApplication`, launch `MainActivity`. |
| `app/src/main/java/com/uit/eousx/EousXApplication.kt` | Hilt application. |
| `app/src/main/java/com/uit/eousx/MainActivity.kt` | Compose content + NavHost. |
| `app/src/main/java/com/uit/eousx/presentation/Screen.kt` | Routes. |
| `app/src/main/java/com/uit/eousx/presentation/SplashScreen.kt` | Splash UI and auto navigation. |
| `app/src/main/java/com/uit/eousx/presentation/HomeScreen.kt` | Home list, bottom tabs, movie cards. |
| `app/src/main/java/com/uit/eousx/presentation/MovieDetailScreen.kt` | Detail UI and inactive booking button. |
| `app/src/main/java/com/uit/eousx/presentation/MovieViewModel.kt` | Fetch popular movies. |
| `app/src/main/java/com/uit/eousx/presentation/MovieDetailViewModel.kt` | Fetches popular movies again and finds by id. |
| `app/src/main/java/com/uit/eousx/data/remote/TmdbApi.kt` | TMDB endpoints. |
| `app/src/main/java/com/uit/eousx/data/remote/MovieResponse.kt` | TMDB DTOs. |
| `app/src/main/java/com/uit/eousx/data/repository/MovieRepositoryImpl.kt` | TMDB repository implementation. |
| `app/src/main/java/com/uit/eousx/domain/model/Movie.kt` | Domain Movie, `id: Int`. |
| `app/src/main/java/com/uit/eousx/domain/repository/MovieRepository.kt` | Movie repository interface. |
| `app/src/main/java/com/uit/eousx/di/NetworkModule.kt` | Moshi, OkHttp, Retrofit, TMDB API providers. |
| `app/src/main/java/com/uit/eousx/di/RepositoryModule.kt` | Repository binding. |
| `app/src/main/java/com/uit/eousx/ui/theme/Color.kt` | EousX color tokens hiện tại. |
| `app/src/main/java/com/uit/eousx/ui/theme/Theme.kt` | Material 3 color schemes. |
| `app/src/main/java/com/uit/eousx/ui/theme/Type.kt` | Typography override duy nhất cho `bodyLarge`. |

## File nên sửa khi bắt đầu integration

- `app/build.gradle.kts`: thêm DataStore nếu chọn DataStore cho token; chỉ apply Google Services khi có `google-services.json` thật.
- `MainActivity.kt`: thêm routes Login/Register/Showtime/Seat/Checkout/Payment/Ticket/History.
- `Screen.kt`: thêm route mới, dùng `String` id cho backend routes.
- `NetworkModule.kt`: thêm backend Retrofit/OkHttp có qualifier hoặc tách `BackendNetworkModule.kt`.
- `HomeScreen.kt`: giữ UI, đổi data source sang backend `/movies` ở phase 3 nếu chọn hướng ưu tiên.
- `MovieDetailScreen.kt`: giữ UI, nối nút đặt vé sang Showtime Selection.
- `MovieViewModel.kt` và `MovieDetailViewModel.kt`: đổi repository hoặc thêm backend-specific viewmodel/repository.
- `RepositoryModule.kt`: bind thêm Auth/Backend repositories.

## File nên tạo mới khi bắt đầu integration

- `data/remote/BackendApi.kt`
- `data/remote/dto/AuthDto.kt`
- `data/remote/dto/BackendMovieDto.kt`
- `data/remote/dto/ShowtimeDto.kt`
- `data/remote/dto/SeatDto.kt`
- `data/remote/dto/BookingDto.kt`
- `data/remote/dto/PaymentDto.kt`
- `data/remote/dto/TicketDto.kt`
- `data/local/TokenManager.kt`
- `data/remote/AuthInterceptor.kt`
- `di/BackendNetworkModule.kt` hoặc qualifiers trong `NetworkModule.kt`
- `domain/repository/AuthRepository.kt`
- `domain/repository/BackendMovieRepository.kt`
- `domain/repository/ShowtimeRepository.kt`
- `domain/repository/SeatRepository.kt`
- `domain/repository/BookingRepository.kt`
- `data/repository/AuthRepositoryImpl.kt`
- `data/repository/BackendMovieRepositoryImpl.kt`
- `data/repository/ShowtimeRepositoryImpl.kt`
- `data/repository/SeatRepositoryImpl.kt`
- `data/repository/BookingRepositoryImpl.kt`
- `presentation/auth/LoginScreen.kt`
- `presentation/auth/RegisterScreen.kt`
- `presentation/auth/AuthViewModel.kt`
- `presentation/showtime/MovieScheduleScreen.kt`
- `presentation/showtime/ShowtimeViewModel.kt`
- `presentation/seat/SeatMapScreen.kt`
- `presentation/seat/SeatMapViewModel.kt`
- `presentation/checkout/CheckoutScreen.kt`
- `presentation/payment/PaymentScreen.kt`
- `presentation/ticket/TicketScreen.kt`
- `presentation/history/BookingHistoryScreen.kt`

## Rủi ro lớn nhất khi nối backend

Rủi ro lớn nhất là mismatch identity giữa TMDB movie id `Int` và backend movie id `String`/UUID. Booking flow backend cần gọi `/movies/:movieId/showtimes`, `/showtimes/:showtimeId/seats`, lock seats và create booking bằng id backend. Nếu Home vẫn dùng TMDB mà không có mapping `tmdbId -> backend movieId`, user có thể chọn phim không đặt vé được.

Rủi ro kế tiếp:

- Nhầm token: dùng Firebase ID token cho booking APIs thay vì EousX backend JWT.
- Nhầm base URL: dùng `localhost:3000` trong emulator thay vì `10.0.2.2:3000`.
- Thêm `/api` prefix sai.
- Gửi `userId` trong customer booking APIs, trái với backend hiện tại.
- Conflict Hilt nếu có hai Retrofit clients mà không qualifier.

## Thứ tự implement đề xuất

1. Tách backend network foundation: `BackendApi`, backend Retrofit, `TokenManager`, `AuthInterceptor`.
2. Làm Auth local trước: Login/Register/AuthViewModel/AuthRepository, lưu EousX JWT, `GET /auth/me`.
3. Đổi movie source sang backend `/movies` cho demo booking ổn định.
4. Showtime Selection bằng backend movie id.
5. Seat Map + lock seats với Bearer token.
6. Checkout + create booking.
7. Mock Payment.
8. Ticket QR/text.
9. Booking History.
10. Polish loading/empty/error/token expired/backend down.

## Có nên đổi Home từ TMDB sang backend `/movies` không?

Nên đổi Home/Movie Detail sang backend `/movies` cho demo booking.

Lý do:

- Booking flow phụ thuộc backend movie id để lấy showtimes.
- Backend là source of truth cho showtime, room, seats, booking, ticket.
- Giảm rủi ro user chọn phim TMDB nhưng backend không có showtime.
- Giữ được UI hiện tại: chỉ cần đổi repository/data source/mapper, không cần rewrite layout.

Chỉ nên giữ TMDB nếu backend có mapping đáng tin cậy `tmdbId -> backendMovieId` và toàn bộ phim TMDB hiển thị trong app đều có dữ liệu backend tương ứng.

## Checklist test sau từng phase

| Phase | Checklist |
|---|---|
| Phase 0 | App build/run như trước; Splash -> Home -> Detail còn hoạt động; không mất UI hiện tại. |
| Phase 1 | Retrofit backend gọi được `http://10.0.2.2:3000/`; không có `/api`; protected request tự gắn Bearer token khi có token; request public không bị lỗi. |
| Phase 2 | Register thành công; Login thành công; token lưu được; restart app vẫn đọc token; `GET /auth/me` trả user; logout/expired token quay về Login. |
| Phase 3 | Home hiển thị phim backend; Detail mở bằng backend movie id; poster/title/overview không vỡ UI; không còn dùng TMDB id cho booking. |
| Phase 4 | Chọn ngày lọc được showtimes; hiển thị giờ, phòng, giá, ghế còn trống; chọn showtime điều hướng đúng `showtimeId`. |
| Phase 5 | Seat map hiển thị AVAILABLE/LOCKED/SOLD/MAINTENANCE/SELECTED; lock seats gửi body chỉ có `seatIds`; không gửi `userId`; nhận `lockIds`, `totalAmount`, `lockedUntil`. |
| Phase 6 | Checkout hiển thị đúng movie/showtime/room/seats/total; countdown giữ ghế chạy; create booking gửi `showtimeId` + `lockIds`; không gửi `userId`. |
| Phase 7 | Mock payment gửi `bookingId` + `provider: MOCK`; booking chuyển PAID; điều hướng sang Ticket. |
| Phase 8 | Ticket gọi `/bookings/:bookingId/ticket`; hiển thị QR text hoặc QR image, movie, room, showtime, seats, total, status. |
| Phase 9 | History gọi `/bookings/me`; không dùng query `userId`; hiển thị WAITING_PAYMENT/PAID/CHECKED_IN/CANCELLED/EXPIRED đúng. |
| Phase 10 | Loading/empty/error rõ; backend down có thông báo; token expired xử lý được; không thêm WebSocket/FCM/AI trước khi REST booking ổn. |
