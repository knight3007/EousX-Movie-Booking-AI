# Android Client Backend Integration Plan

Mục tiêu: nối Android app với backend EousX hiện tại mà không rewrite toàn bộ app, không phá UI Home/Movie Detail đang có.

Backend local:

- Máy host: `http://localhost:3000`
- Android emulator: `http://10.0.2.2:3000/`
- Không dùng `/api` prefix.

Auth rule:

- Backend login/register/google trả EousX backend JWT.
- Android lưu EousX JWT.
- Customer protected APIs dùng `Authorization: Bearer EOUSX_BACKEND_JWT`.
- Firebase ID Token chỉ dùng cho `POST /auth/google`.
- Không gửi `userId` trong body/query cho customer booking flow.

## Phase 0 - Giữ ổn định app hiện tại

Mục tiêu:

- Không rewrite toàn bộ app.
- Giữ UI Home/Movie Detail nếu đang chạy ổn.
- Chỉ thêm backend layer song song với TMDB layer nếu cần.
- Không xóa `HomeScreen.kt` hoặc `MovieDetailScreen.kt`.
- Không đổi package name `com.uit.eousx`.

Việc cần làm:

- Tạo branch/checkpoint trước khi sửa code.
- Build app hiện tại để có baseline.
- Ghi nhận flow hiện tại: Splash -> Home -> Movie Detail.
- Không thay Splash animation, Home card layout, Detail layout trong phase này.

## Phase 1 - Backend API foundation

Tạo nền tảng backend networking riêng, tránh làm hỏng TMDB client hiện tại.

Files nên tạo:

- `data/remote/BackendApi.kt`
- `data/remote/AuthInterceptor.kt`
- `data/local/TokenManager.kt`
- `di/BackendNetworkModule.kt`

API foundation:

- `BackendApi.kt` khai báo backend endpoints.
- `BackendNetworkModule` hoặc mở rộng `NetworkModule` với qualifier rõ ràng.
- `AuthInterceptor` tự động gắn `Authorization: Bearer <token>` cho protected APIs.
- `TokenManager` dùng DataStore hoặc SharedPreferences.

Base URL:

- Debug emulator: `http://10.0.2.2:3000/`
- Local physical device: LAN IP của máy chạy backend, ví dụ `http://192.168.x.x:3000/`
- Không dùng `http://localhost:3000/` trong emulator.
- Không thêm `/api`.

Hilt lưu ý:

- Nếu giữ cả TMDB và backend Retrofit, phải dùng qualifier như `@TmdbRetrofit`, `@BackendRetrofit`, `@BackendOkHttpClient`.
- TMDB interceptor hiện đang gắn TMDB token. Backend cần OkHttp client khác để gắn EousX JWT.

## Phase 2 - Auth

Mục tiêu: có login/register thật và token backend dùng được cho booking APIs.

Files nên tạo:

- `presentation/auth/LoginScreen.kt`
- `presentation/auth/RegisterScreen.kt`
- `presentation/auth/AuthViewModel.kt`
- `domain/repository/AuthRepository.kt`
- `data/repository/AuthRepositoryImpl.kt`
- DTOs trong `data/remote/dto/AuthDto.kt`

Local auth APIs:

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`

Google auth:

1. Firebase Auth lấy Firebase ID Token.
2. Android gọi `POST /auth/google` với Firebase ID Token.
3. Backend trả EousX JWT.
4. Android lưu EousX JWT.
5. Các API booking sau đó dùng EousX JWT, không dùng Firebase token trực tiếp.

Navigation:

- Splash kiểm tra token:
  - Có token và `GET /auth/me` thành công -> Home.
  - Không có token hoặc token hết hạn -> Login.
- Sau login/register thành công -> Home.

Không làm:

- Không hardcode token.
- Không gửi Firebase service account vào Android.
- Không commit `google-services.json` nếu chứa config nhạy cảm ngoài chính sách repo.

## Phase 3 - Movie source strategy

### Hướng ưu tiên cho demo: đổi Home/Movie Detail sang backend `/movies`

Nên chọn hướng này cho đồ án/demo booking.

Lý do:

- Backend movie id là id cần dùng để gọi `/movies/:movieId/showtimes`.
- Backend là source of truth cho showtime, room, seats, booking, payment, ticket.
- Giảm mismatch giữa TMDB id `Int` và backend id `String`/UUID.
- Vẫn giữ lại UI hiện có, chỉ đổi data source và mapper.

Việc cần làm:

- Thêm backend movie DTO/model.
- Tạo `BackendMovieRepository`.
- Đổi Home view model hoặc tạo view model mới gọi backend `/movies`.
- Đổi route detail từ `Int` sang `String` nếu backend id là UUID/string.
- Mapper backend movie -> UI model để giữ layout hiện tại.

### Hướng phụ nếu muốn giữ TMDB

Chỉ chọn nếu backend có mapping rõ ràng:

- Home vẫn browse TMDB.
- Booking chỉ hoạt động với phim đã có trong backend.
- Cần mapping `tmdbId -> backendMovieId` nếu backend có field `tmdbId`.
- Nếu chưa có mapping chắc chắn thì không chọn hướng này.

## Phase 4 - Showtime Selection

Files nên tạo:

- `presentation/showtime/MovieScheduleScreen.kt`
- `presentation/showtime/ShowtimeViewModel.kt`
- `domain/repository/ShowtimeRepository.kt`
- `data/repository/ShowtimeRepositoryImpl.kt`
- `data/remote/dto/ShowtimeDto.kt`

APIs:

- `GET /movies/:movieId/showtimes`
- `GET /movies/:movieId/showtimes?date=YYYY-MM-DD`

UI:

- Hiển thị ngày.
- Hiển thị giờ chiếu.
- Hiển thị phòng.
- Hiển thị giá vé.
- Hiển thị số ghế còn trống.
- User chọn showtime -> điều hướng `SeatMapScreen(showtimeId)`.

## Phase 5 - Seat Map + Lock seats

Files nên tạo:

- `presentation/seat/SeatMapScreen.kt`
- `presentation/seat/SeatMapViewModel.kt`
- `data/remote/dto/SeatDto.kt`
- domain model `Seat`

APIs:

- `GET /showtimes/:showtimeId/seats`
- `POST /showtimes/:showtimeId/seat-locks`

Body lock seats:

```json
{
  "seatIds": ["..."]
}
```

Yêu cầu bắt buộc:

- Không gửi `userId`.
- Header phải có `Authorization: Bearer EOUSX_BACKEND_JWT`.

Trạng thái ghế cần hiển thị:

- `AVAILABLE`
- `LOCKED`
- `SOLD`
- `MAINTENANCE`
- `SELECTED`

Sau lock thành công:

- Nhận `lockIds`.
- Nhận `totalAmount`.
- Nhận `lockedUntil`.
- Điều hướng sang `CheckoutScreen`.

## Phase 6 - Checkout + Booking

Files nên tạo:

- `presentation/checkout/CheckoutScreen.kt`
- Có thể dùng chung `SeatMapViewModel` state hoặc tạo `CheckoutViewModel`.
- `data/remote/dto/BookingDto.kt`

UI:

- Movie.
- Showtime.
- Room.
- Seats.
- Total amount.
- Countdown giữ ghế.

API:

- `POST /bookings`

Body:

```json
{
  "showtimeId": "...",
  "lockIds": ["..."]
}
```

Yêu cầu:

- Không gửi `userId`.
- Thành công -> nhận `bookingId` -> sang `PaymentScreen`.

## Phase 7 - Mock Payment

Files nên tạo:

- `presentation/payment/PaymentScreen.kt`
- `data/remote/dto/PaymentDto.kt`

Với đồ án, chỉ làm mock payment trước.

API:

- `POST /payments/mock-success`

Body:

```json
{
  "bookingId": "...",
  "provider": "MOCK"
}
```

Kết quả:

- Thành công -> booking `PAID`.
- Ticket được tạo.
- Điều hướng `TicketScreen`.

## Phase 8 - Ticket QR

Files nên tạo:

- `presentation/ticket/TicketScreen.kt`
- `data/remote/dto/TicketDto.kt`

API:

- `GET /bookings/:bookingId/ticket`

UI hiển thị:

- QR code hoặc QR text.
- Movie.
- Room.
- Showtime.
- Seats.
- Total amount.
- Ticket status.

Nếu chưa có thư viện QR:

- Trước mắt hiển thị `qrCode` dạng text.
- Thêm copy button.
- Sau đó mới thêm QR image library.

## Phase 9 - Booking History

Files nên tạo:

- `presentation/history/BookingHistoryScreen.kt`
- Có thể dùng `BookingRepository`.

API:

- `GET /bookings/me`

Yêu cầu:

- Không dùng `?userId`.
- Header `Authorization: Bearer EOUSX_BACKEND_JWT`.

Trạng thái booking cần hiển thị:

- `WAITING_PAYMENT`
- `PAID`
- `CHECKED_IN`
- `CANCELLED`
- `EXPIRED`

## Phase 10 - Polish

Tập trung độ ổn định và UX cơ bản:

- Loading state.
- Empty state.
- Error state.
- Token expired -> quay về Login.
- Backend không chạy -> thông báo rõ.
- Retry button cho lỗi network.
- Disable nút khi request đang chạy.
- Format tiền và ngày giờ thống nhất.

Không làm trong phase này nếu REST booking chưa ổn:

- WebSocket.
- FCM.
- AI chatbot.

## Route đề xuất

| Screen | Route đề xuất |
|---|---|
| Splash | `splash_screen` |
| Login | `login_screen` |
| Register | `register_screen` |
| Home | `home_screen` |
| Movie Detail | `movie_detail/{movieId}` với `movieId: String` |
| Showtime Selection | `movie_schedule/{movieId}` |
| Seat Map | `seat_map/{showtimeId}` |
| Checkout | `checkout/{showtimeId}` hoặc dùng shared state/saved state cho lock result |
| Payment | `payment/{bookingId}` |
| Ticket | `ticket/{bookingId}` |
| Booking History | `booking_history` |

## Checklist bắt buộc khi implement

- Không xóa `HomeScreen.kt`.
- Không xóa `MovieDetailScreen.kt`.
- Không rewrite toàn bộ architecture.
- Không đổi package name.
- Không hardcode token.
- Không commit secret.
- Không đưa Firebase service account vào Android.
- Không thêm `/api` vào backend base URL.
- Không dùng `localhost:3000` trong Android emulator.
- Không gửi `userId` trong customer booking APIs.
- Không dùng Firebase token trực tiếp cho booking APIs.
- Không làm WebSocket/FCM/AI trong lần đầu nếu booking flow REST chưa ổn.
