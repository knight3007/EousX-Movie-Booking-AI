# EousX Project Retrospective & Technical Knowledge Base

## 1. Mục tiêu tài liệu

Tài liệu này tổng kết project EousX Movie Booking AI dựa trên source code, README, docs, schema, seed và test script hiện có trong repo. Mục tiêu không chỉ mô tả đồ án cũ, mà rút ra các nguyên tắc kỹ thuật có thể tái sử dụng cho project cá nhân tiếp theo: thiết kế backend làm source of truth, quản lý auth/token, kiểm soát luồng booking/payment/ticket, xử lý lock tài nguyên có TTL, tổ chức client theo API layer/repository/UI state, và giới hạn scope admin để phục vụ vận hành/demo.

## 2. Executive Summary

- EousX giải quyết bài toán đặt vé xem phim: người dùng đăng nhập, chọn phim, chọn suất chiếu, chọn ghế, khóa ghế tạm thời, tạo booking, thanh toán, nhận vé và xem lịch sử.
- Người dùng chính gồm customer trên Android app và operator/staff trên Admin Web.
- Hệ thống gồm ba phần chính: NestJS API server, Android customer app và React Admin Web.
- Flow nghiệp vụ quan trọng nhất: Auth -> Movies -> Movie Detail -> Showtime -> Seat Map -> Seat Lock -> Checkout -> Booking -> Payment -> Ticket -> Booking History -> Ticket Check-in/Admin monitor.
- Giá trị kỹ thuật lớn nhất của project là đưa nghiệp vụ nhạy cảm về backend: user identity lấy từ JWT, trạng thái ghế tính theo showtime, booking sinh từ lock hợp lệ, payment/ticket được xác nhận ở backend, client chỉ điều phối UI.

## 3. Cấu trúc repo hiện tại

```text
EousX-Movie-Booking-AI/
  api-server/                 # NestJS API, Prisma, PostgreSQL, auth, booking, payment, ticket, AI
    prisma/
      schema.prisma
      seed.ts
      migrations/
    scripts/
      api-smoke-test.js
    src/
      auth/
      movies/
      rooms/
      showtimes/
      seats/
      seat-locks/
      bookings/
      payments/
      tickets/
      ai/
      firebase/
      database/
  client-app/                 # Android app module :app
    app/src/main/java/com/uit/eousx/
      core/
      data/
      di/
      domain/
      presentation/
    gradle/libs.versions.toml
  admin-web/                  # React + Vite admin dashboard
    src/
      features/
      shared/api/
      types/
  docs/                       # Payment/manual docs và retrospective này
  README.md
```

- `api-server/`: business logic và source of truth cho auth, movie, room, showtime, seat map, seat lock, booking, payment, ticket, AI chat.
- `client-app/`: Android customer flow bằng Kotlin/Compose/Hilt/Retrofit/StateFlow/DataStore.
- `admin-web/`: dashboard vận hành bằng React/Vite/TypeScript/TanStack Query/Axios.
- `docs/`: có tài liệu SePay backend, checklist manual SePay và một số audit Android/UI kit.
- `api-server/docker-compose.yml`: PostgreSQL local.
- `api-server/.env.example`: có biến database, JWT, Firebase Admin, SePay.

## 4. Kiến trúc tổng quan hệ thống

```text
Android App --------\
                    -> NestJS API Server -> Prisma -> PostgreSQL
Admin Web ----------/        |
                             +-> Firebase Admin verifies Google ID token
                             +-> Gemini API through /ai/chat
                             +-> SePay webhook/payment confirmation
```

- Android và Admin Web gọi REST API trực tiếp, backend hiện không dùng prefix `/api`.
- Backend là source of truth cho User, Movie, Cinema, Room, Seat, Showtime, SeatLock, Booking, Payment, Ticket.
- Seat không lưu trạng thái SOLD toàn cục; trạng thái hiển thị theo showtime được tính từ `BookingSeat` của booking đã `PAID/CHECKED_IN` và `SeatLock` còn active.
- Payment provider nằm ở backend: mock payment gọi endpoint backend; SePay tạo payment/QR ở backend và xác nhận bằng webhook.
- Android/Admin Web không nên tự quyết định `userId`, trạng thái paid, trạng thái sold hoặc check-in hợp lệ. Các hành động này phải đi qua backend.

## 5. Domain nghiệp vụ cốt lõi

| Entity | Vai trò | Quan hệ/trạng thái | Bài học thiết kế lại |
|---|---|---|---|
| `User` | Customer account | Có `bookings`, `seatLocks`, email unique, optional `firebaseUid` | Không nhận `userId` từ client; lấy từ JWT. |
| `Movie` | Metadata phim | Có nhiều `Showtime`; status `NOW_SHOWING/UPCOMING/ENDED` | Nếu client cần booking, dùng backend movie id làm id chính. |
| `Cinema` | Rạp | Có nhiều `Room` | Với MVP có thể seed một cinema trước. |
| `Room` | Phòng chiếu | Có `Seat`, `Showtime`; type `STANDARD_2D/VIP/COUPLE` | Room/seat layout nên seed hoặc quản trị riêng, tránh làm phình scope admin sớm. |
| `Seat` | Ghế vật lý | Thuộc `Room`; unique `[roomId, code]`; type `STANDARD/VIP/COUPLE/DISABLED/MAINTENANCE`; `isActive` | Không dùng một field sold global vì ghế sold theo từng showtime. |
| `Showtime` | Suất chiếu | Thuộc `Movie` và `Room`; status `OPEN/CLOSED/CANCELLED` | Cần chống trùng lịch phòng bằng kiểm tra overlap. |
| `SeatLock` | Giữ ghế tạm thời | Thuộc user/showtime/seat; status `ACTIVE/EXPIRED/RELEASED/CONVERTED_TO_BOOKING`; `lockedUntil` | Lock phải có TTL và cleanup trước khi đọc/ghi nghiệp vụ. |
| `Booking` | Đơn đặt vé | Thuộc user/showtime; status `PENDING/WAITING_PAYMENT/PAID/EXPIRED/CANCELLED/REFUNDED/CHECKED_IN` | Booking nên được tạo từ lock hợp lệ, không từ seatIds tự do. |
| `BookingSeat` | Ghế trong booking | Unique `[bookingId, seatId]`; lưu price tại thời điểm booking | Lưu giá snapshot để tránh thay đổi showtime price ảnh hưởng booking cũ. |
| `Payment` | Thanh toán | One-to-one với booking; provider/transaction/status | Production cần idempotency, reconciliation, log webhook. |
| `Ticket` | Vé/QR/check-in | One-to-one với booking; `qrCode` unique; status `VALID/USED/CANCELLED/EXPIRED` | Ticket chỉ nên sinh sau payment thành công. |

## 6. Backend API Server

Tech stack đã xác minh: Node.js, TypeScript, NestJS, Prisma, PostgreSQL, JWT, Firebase Admin, Gemini API, Docker Compose. `main.ts` bật CORS và `ValidationPipe` với `whitelist`, `transform`, `forbidNonWhitelisted`.

| Nhóm API | Endpoint chính | Protected/Public | Input chính | Output chính | Ý nghĩa/rủi ro |
|---|---|---|---|---|---|
| Health | `GET /health` | Public | none | health payload | Dùng smoke/demo. |
| Auth | `POST /auth/register`, `/auth/login`, `/auth/google`, `GET /auth/me` | `me` protected | email/password hoặc Firebase ID token | EousX backend JWT + user | Firebase ID token chỉ dùng để đổi sang backend JWT. |
| Movies | `GET /movies`, `/movies/now-showing`, `/movies/upcoming`, `/movies/:id` | Public | id/status | movie data | Admin CRUD public trong demo. |
| Admin Movies | `POST/PUT/DELETE /admin/movies` | Public hiện tại | `CreateMovieDto`, `UpdateMovieDto` | movie | Production cần admin guard. |
| Rooms | `GET /rooms`, `/rooms/:id`, `/admin/rooms`, `/admin/rooms/:id` | Public | room id | room/seats | Room CRUD chưa có trong code hiện tại. |
| Showtimes | `GET /showtimes`, `/showtimes/:id`, `/movies/:movieId/showtimes?date=` | Public | date optional | showtime list/detail | `findByMovie` chỉ lấy `OPEN` và tính available seats. |
| Admin Showtimes | `GET/POST/PUT/DELETE /admin/showtimes` | Public hiện tại | movieId, roomId, start/end, basePrice, status | showtime | Service có kiểm tra room schedule conflict; delete là cancel. |
| Seats | `GET /showtimes/:showtimeId/seats`, `/admin/showtimes/:showtimeId/seats` | Public | showtimeId | seat map, summary | Cleanup lock expired trước khi trả map. |
| Seat Locks | `POST /showtimes/:showtimeId/seat-locks`, `GET /seat-locks/:lockId`, `DELETE /seat-locks/:lockId` | POST/DELETE protected; GET public | `seatIds`, lockId | locks, TTL, totalAmount | Lock tạo trong transaction nhưng chưa có unique active-lock constraint ở DB. |
| Bookings | `POST /bookings`, `GET /bookings/me`, `GET /bookings/:id`, `PATCH /bookings/:id/cancel` | Protected | `showtimeId`, `lockIds` | booking | Ownership lấy từ JWT; tạo booking trong transaction. |
| Admin Bookings | `GET /admin/bookings`, `/admin/bookings/:id` | Public hiện tại | id optional | booking detail | Production cần admin auth. |
| Mock Payment | `POST /payments/mock-success` | Protected | `bookingId`, provider optional | booking/payment/ticket | Kiểm tra lock còn hiệu lực trước khi paid. |
| SePay | `POST /payments/sepay/create`, `POST /payments/webhook/sepay`, `GET /payments/sepay/status/:paymentCode` | create/status protected; webhook public + API key | bookingId/paymentCode/webhook payload | QR/status/update result | Android không tự confirm payment; webhook match code + amount. |
| Payment read | `GET /payments/:id/status`, `GET /bookings/:bookingId/payment` | payment by booking protected; payment status endpoint public hiện tại | id/bookingId | payment | `GET /payments/:id/status` chưa có guard. |
| Tickets | `GET /bookings/:bookingId/ticket`, `GET /tickets/verify/:qrCode`, `POST /tickets/:ticketId/check-in` | booking ticket protected; verify/check-in public hiện tại | bookingId/qrCode/ticketId | ticket/verify result | Check-in nên là staff/admin-only trong production. |
| AI | `POST /ai/chat` | Protected | message | AI response/recommendations | Backend gọi Gemini; client không giữ API key. |

Transaction/database consistency đã xác minh trong `SeatLocksService.lockSeats`, `BookingsService.create`, `PaymentsService.mockSuccess`, `PaymentsService.handleSepayWebhook`. Các transaction này gom kiểm tra điều kiện và cập nhật trạng thái trong cùng một khối Prisma.

## 7. Authentication & Authorization

- Local register/login dùng email normalized lowercase và bcrypt hash. Login có fallback nâng cấp seed password plaintext nếu gặp dữ liệu cũ.
- Google login: Android lấy Firebase ID token, backend verify qua Firebase Admin, tìm/tạo user rồi trả EousX backend JWT.
- JWT payload chứa `sub = user.id`, `email`; `JwtAuthGuard` verify Bearer token, load user từ DB và gắn `request.user`.
- Android lưu backend access token bằng DataStore trong `TokenManager`, `AuthInterceptor` tự gắn `Authorization: Bearer <token>`.
- Customer APIs như lock seat, create booking, booking history, cancel booking, mock payment, SePay create/status, booking ticket, AI chat đều dùng JWT trong code hiện tại.
- Không nên gửi `userId` từ client vì client có thể giả mạo id khác. Backend đã lấy user từ JWT ở các flow customer chính.
- Public/demo còn lại: admin CRUD/read routes, ticket verify/check-in, `GET /payments/:id/status`, `GET /seat-locks/:lockId`. Production cần guard/role.

## 8. Seat Locking Design

- Lock ghế cần thiết để tránh hai user cùng thanh toán một ghế trong cùng showtime.
- TTL đã xác minh: `LOCK_DURATION_MINUTES = 3` trong `api-server/src/seat-locks/seat-locks.service.ts`; seed có một lock demo 10 phút nhưng service runtime lock 3 phút.
- `lockSeats` kiểm tra user tồn tại, showtime tồn tại và `OPEN`, dọn lock hết hạn, seat tồn tại, seat thuộc đúng room, seat active, không `DISABLED/MAINTENANCE`, chưa sold bởi booking `PAID/CHECKED_IN`, chưa bị active lock khác giữ.
- Seat map tự đánh dấu active lock hết hạn thành `EXPIRED` trước khi tính trạng thái.
- Booking được tạo từ `lockIds` active, đúng user, đúng showtime, chưa hết hạn. Sau mock payment thành công, service kiểm tra lock còn hiệu lực rồi chuyển lock sang `CONVERTED_TO_BOOKING`. Với SePay webhook, code hiện tại chuyển các lock còn `ACTIVE` sang `CONVERTED_TO_BOOKING`, nhưng chưa thấy kiểm tra `lockedUntil` như mock payment.
- Race condition còn cần chú ý: schema chưa có unique constraint kiểu “một active lock cho một seat/showtime” hoặc “một paid booking seat cho seat/showtime” ở DB level. Hiện code kiểm tra trong transaction, nhưng dưới concurrency cao vẫn nên bổ sung constraint/locking strategy hoặc isolation phù hợp.
- Project mới nên thêm cleanup job định kỳ, idempotency key cho lock/booking/payment và test concurrency cho cùng seat.

## 9. Booking Flow

End-to-end hiện tại:

1. User login/register hoặc Google login để có backend JWT.
2. Android lấy phim từ `/movies`.
3. User mở movie detail và chọn showtime từ `/movies/:movieId/showtimes`.
4. Android lấy seat map từ `/showtimes/:showtimeId/seats`.
5. User chọn ghế và gọi `POST /showtimes/:showtimeId/seat-locks`.
6. Checkout nhận `lockIds`, `lockedUntil`, `totalAmount`, seat codes.
7. Android gọi `POST /bookings` với `showtimeId` và `lockIds`.
8. Booking tạo ra ở trạng thái `WAITING_PAYMENT`.
9. User thanh toán bằng mock hoặc SePay.
10. Backend chuyển booking sang `PAID`, tạo/upsert `Payment`, tạo/upsert `Ticket`.
11. Android gọi `/bookings/:bookingId/ticket` và `/bookings/me`.
12. Staff/Admin verify QR và check-in, backend chuyển ticket `USED`, booking `CHECKED_IN`.

Booking statuses thật trong schema: `PENDING`, `WAITING_PAYMENT`, `PAID`, `EXPIRED`, `CANCELLED`, `REFUNDED`, `CHECKED_IN`. Code tạo booking với `WAITING_PAYMENT`; mock payment có thể đánh dấu `EXPIRED` nếu lock hết hạn; cancel chuyển `CANCELLED`; ticket check-in chuyển `CHECKED_IN`.

## 10. Payment Flow

### 10.1 Mock Payment

- Mục đích: demo nhanh và fallback khi chưa dùng provider thật.
- API: `POST /payments/mock-success` protected JWT.
- Body: `bookingId`, optional `provider`.
- Điều kiện thành công: booking thuộc user, không cancelled/expired, đang `WAITING_PAYMENT`, có seats, lock tương ứng còn active và chưa hết hạn, ghế chưa bị booking khác `PAID/CHECKED_IN`.
- Backend upsert `Payment` với `status = SUCCESS`, `paidAt = now`; upsert `Ticket` với `status = VALID`; update `Booking.status = PAID`; chuyển active locks sang `CONVERTED_TO_BOOKING`.

### 10.2 SePay Payment nếu có

- Đã có code và docs SePay. Public domain trong docs: `https://api-eousx.hius.io.vn`, webhook URL `/payments/webhook/sepay`.
- Create API: `POST /payments/sepay/create` protected JWT, body `bookingId`.
- Webhook API: `POST /payments/webhook/sepay`, public nhưng xác thực bằng `SEPAY_WEBHOOK_API_KEY` qua `Authorization`, `Bearer`, `ApiKey` hoặc `x-api-key`.
- Status polling API: `GET /payments/sepay/status/:paymentCode` protected JWT.
- Payment code format: `EOUSXYYYYMMDDXXXXXX`, regex match `\bEOUSX\d{8}[A-Z0-9]{6}\b`.
- Webhook chỉ xử lý `transferType = in`, extract paymentCode từ `content` hoặc `description`, kiểm tra `transferAmount === booking.totalAmount`, bỏ qua nếu không match hoặc đã confirmed.
- Android không tự xác nhận payment vì nếu client được quyền đổi booking sang paid thì có thể giả mạo thanh toán. Android chỉ tạo QR và polling status.
- Rủi ro production: chưa có reconciliation job, payment logs/admin dashboard riêng, retry nội bộ, kiểm soát replay nâng cao, kiểm thử provider production.
- Điểm cần chú ý về consistency: `mockSuccess` kiểm tra active locks còn hạn trước khi paid; `handleSepayWebhook` hiện kiểm tra booking `WAITING_PAYMENT`, amount và ghế chưa bị booking khác paid, nhưng chưa thấy kiểm tra lock còn hạn trước khi chuyển booking sang `PAID`.

## 11. Ticket & Check-in Flow

- Ticket được tạo trong mock payment success hoặc SePay webhook success.
- `Ticket.qrCode` lưu string unique trong DB. Android `TicketScreen` hiện hiển thị chuỗi `qrCode`; QR image đã thấy trong `PaymentScreen` cho SePay QR trước khi ticket được mở, chưa phải QR bitmap của ticket.
- Ticket statuses: `VALID`, `USED`, `CANCELLED`, `EXPIRED`.
- Verify ticket trả các case: `INVALID` nếu không tìm thấy, `ALREADY_USED` nếu status `USED`, status hiện tại nếu không `VALID`, hoặc `VALID`.
- Check-in yêu cầu ticket tồn tại, status `VALID`, booking status `PAID`; sau đó update ticket `USED`, `usedAt`, và booking `CHECKED_IN`.
- Check-in nên là staff/admin function vì nó thay đổi trạng thái vé khi khách vào rạp. Code hiện tại route check-in public, Admin Web gọi trực tiếp, production cần admin auth/role.

## 12. Android Client

Tech stack đã xác minh: Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit, Moshi, OkHttp, Coroutines/StateFlow, DataStore, Firebase Auth/Google Sign-In dependency, Coil, Media3, Android Room dependency. Chưa xác minh được Room database/DAO được dùng thật trong source hiện tại.

- Folder structure: `core` chứa navigation/network/storage/utils; `data` chứa API/DTO/repository impl; `domain` chứa model/repository interfaces; `di` chứa Hilt modules; `presentation` chứa screens, ViewModels, reusable components.
- Navigation: `AppNavGraph` start ở Splash, sau đó Login/Register/Home/AI/MovieDetail/MovieSchedule/SeatMap/Checkout/Payment/Ticket.
- Backend connection: `BackendNetworkModule` dùng base URL `https://api-eousx.hius.io.vn/`; `BackendApi` dùng route không `/api`.
- Token storage: `TokenManager` lưu `access_token` trong DataStore; `AuthInterceptor` gắn Bearer token.
- Pattern: Repository interface trong `domain/repository`, implementation trong `data/repository`, ViewModel giữ `StateFlow` UI state, UI Compose collect state.
- Các màn hình hiện có trong source: Splash, Login, Register, Home, Movie Detail, Movie Schedule, Seat Map, Checkout, Payment, Ticket, Booking History, Profile, AI Chat.
- Flow MVP đã có trong code: auth, movie list/detail, showtime, seat polling/lock, checkout/create booking, mock payment, SePay create/polling, ticket, booking history, AI chat.
- Docs Android cũ còn nói TMDB là nguồn dữ liệu; source hiện tại cho thấy app đã có `BackendApi`, backend DTO/repository và backend movie id `String`. Khi docs khác source, source hiện tại là nguồn đúng hơn.
- Nên giữ cho project sau: Hilt module tách backend client, repository pattern, `NetworkResult`, `ApiErrorHandler`, DataStore token, UI state bằng StateFlow, route id dạng String/encoded.
- Nên tránh: hardcode production base URL trong source nếu cần nhiều môi trường; để payment instruction quá thủ công trong UI; để docs audit cũ không cập nhật sau khi integration hoàn tất.

## 13. Android UI/UX & UI Kit

- UI kit nằm trong `client-app/app/src/main/java/com/uit/eousx/presentation/components`.
- Thành phần đã xác minh: `EousXTheme`, `EousXButtons`, `EousXTextField`, `EousXStatusChip`, `EousXState`, `EousXDialog`, `EousXSeat`, `EousXShowtime`, `EousXCheckout`, `EousXMovieCard`, `EousXTicketCard`, `EousXBottomNavigation`.
- Theme Android UI kit dùng dark cinema style với tokens màu/spacing/radius/typography trong `EousXTheme.kt`.
- Seat component hỗ trợ các trạng thái từ backend: `AVAILABLE`, `LOCKED`, `SOLD`, `MAINTENANCE`.
- Payment screen có chọn MOCK/SEPAY, SePay QR card bằng `AsyncImage`, button check status.
- Ticket screen hiện render `ticket.qrCode` dạng text. `EousXTicketCard` có slot cho QR content, nhưng QR bitmap rendering hoàn chỉnh cho ticket chưa xác minh được trong source hiện tại.
- Bài học: làm UI kit sớm giúp các screen booking/payment/ticket dùng cùng trạng thái loading/error/empty, chip/status, button và seat visual; nhưng cần giữ UI kit thuần UI, không nhúng DTO/backend logic.

## 14. Admin Web

- Mục tiêu: dashboard/operator side cho demo cinema flow.
- Tech stack: React, Vite, TypeScript, Tailwind CSS, Axios, React Router, TanStack Query, Lucide React.
- Route/page structure trong `App.tsx`: Dashboard, Movies, Showtimes, Seat Monitor, Bookings, Ticket Check-in, Rooms placeholder.
- API client: `apiClient.ts` dùng `VITE_API_BASE_URL` hoặc fallback `http://localhost:3000`, timeout 15s.
- Pages:
  - Dashboard: summary bookings/showtimes, revenue mock, sold tickets, checked-in, top movies.
  - Movies: CRUD qua `/admin/movies`.
  - Showtimes: CRUD/cancel qua `/admin/showtimes`.
  - Seat Monitor: chọn showtime, gọi `/admin/showtimes/:showtimeId/seats`, polling 5 giây khi auto-refresh on.
  - Bookings: list/detail qua `/admin/bookings`.
  - Ticket Check-in: verify QR và confirm check-in qua `/tickets/verify/:qrCode`, `/tickets/:ticketId/check-in`.
  - Rooms: placeholder/read-only.
- Phạm vi giảm tải đã xác minh: room CRUD không có UI đầy đủ; room page placeholder; chưa có admin auth; chưa có WebSocket realtime; seat monitor dùng polling.
- Khuyến nghị production: admin login/role guard, audit log, permission theo role, confirmation mạnh hơn cho check-in/cancel/delete, realtime hoặc event stream nếu volume cao.

## 15. Database & Prisma

- Provider: PostgreSQL; Prisma client generator.
- Schema chính gồm `Cinema`, `Room`, `Seat`, `Movie`, `Showtime`, `User`, `Admin`, `SeatLock`, `Booking`, `BookingSeat`, `Payment`, `Ticket`.
- Unique/index quan trọng:
  - `User.email`, `User.firebaseUid`, `Admin.email`
  - `Seat @@unique([roomId, code])`
  - `Booking.code`
  - `BookingSeat @@unique([bookingId, seatId])`
  - `Payment.bookingId`, `Ticket.bookingId`, `Ticket.qrCode`
  - Index cho `cinemaId`, `roomId`, `movieId`, `startTime`, `userId`, `showtimeId`, `seatId`
- Transaction dùng ở seat lock, booking create, mock payment, SePay webhook.
- Seed data tạo một cinema, ba room, 124 seats, movie now showing/upcoming, showtimes ngày mai, demo user, admin, paid booking, payment, ticket, active lock và một maintenance seat.
- Điểm mạnh: domain rõ, quan hệ one-to-one payment/ticket, status enum đầy đủ, seat map tính theo showtime.
- Điểm yếu/rủi ro: thiếu constraint chống double-sell theo `(showtimeId, seatId)` ở DB vì `BookingSeat` không chứa `showtimeId`; thiếu unique partial active lock; admin model có nhưng chưa dùng auth; password/JWT dev secret trong `api-server/.env.example` chỉ phù hợp local.

## 16. Testing & Demo Readiness

- Backend scripts: `npm run build`, `npm run test:api`, `npm run start:dev`, `npm run prisma:generate`, `npm run prisma:migrate`, `npm run seed`.
- Smoke test: `api-server/scripts/api-smoke-test.js` chạy flow health, auth, Google optional, movies, rooms, showtimes, seats, seat locks, bookings, payments, tickets, admin support. Script có thể dùng `EOUSX_API_BASE_URL`, mặc định public domain, và ghi `api-smoke-report.json`.
- Android build/test: README ghi `.\gradlew.bat :app:assembleDebug`; repo có unit test `ShowtimeTimeFormatterTest.kt` và template tests.
- Admin Web scripts: `npm run dev`, `npm run build`, `npm run lint`, `npm run preview`.
- Manual checklist: `docs/sepay-payment-manual-test-checklist.md` mô tả test SePay create/status/webhook/wrong amount/wrong code/transfer out/duplicate webhook.
- Demo nên chuẩn bị: database migrated + seeded, backend env, Firebase service account nếu test Google, SePay env nếu test real QR/webhook, admin `.env`, Android Google config nếu dùng Google sign-in.

## 17. Công cụ và thư viện đã dùng

| Công cụ/thư viện | Khu vực | Vai trò | Lý do dùng | Tái sử dụng? | Ghi chú/rủi ro |
|---|---|---|---|---|---|
| NestJS | Backend | Module/controller/service API | Structure rõ cho REST | Có | Cần guard admin production. |
| Prisma | Backend/DB | ORM/schema/migration/seed | Type-safe DB access | Có | Cần constraint cho concurrency. |
| PostgreSQL | Backend/DB | Relational source of truth | Phù hợp booking/payment | Có | Cần migration discipline. |
| JWT | Backend/client | Auth session | Stateless API auth | Có | Secret/token expiry phải quản lý kỹ. |
| Firebase Admin/Auth | Auth | Google login verification | Không tự verify token ở client | Có nếu cần social login | Config secret không commit. |
| bcryptjs | Backend | Password hashing | Local auth | Có | Không dùng plaintext seed password lâu dài. |
| class-validator | Backend | DTO validation | Chặn input thừa/sai | Có | DTO cần coverage đầy đủ. |
| Gemini `@google/genai` | Backend AI | AI chat/recommendation | Client không giữ AI key | Có nếu AI cần thiết | Cần fallback và sanitize. |
| Kotlin/Compose | Android | Native UI | Reactive UI | Có | Cần state rõ để tránh navigation phức tạp. |
| Hilt | Android | DI | Tách API/repository/ViewModel | Có | Dùng qualifier khi nhiều client. |
| Retrofit/Moshi/OkHttp | Android | REST client | Chuẩn Android API layer | Có | Không hardcode env production. |
| DataStore | Android | Token storage | Flow-friendly local storage | Có | Không lưu secret dài hạn nhạy cảm. |
| StateFlow/Coroutines | Android | UI state async | Dễ test và render | Có | Tránh side effect trong UI. |
| React/Vite/TypeScript | Admin | Web dashboard | Build nhanh, typed UI | Có | Vite env không chứa secret. |
| TanStack Query | Admin | Fetch/cache/polling | Seat monitor/dashboard tiện | Có | Cần invalidate/refetch đúng. |
| Axios | Admin | HTTP client | API abstraction đơn giản | Có | Cần auth interceptor khi có admin login. |
| Tailwind CSS | Admin | Styling | Tốc độ triển khai | Có | Duy trì design tokens. |
| Docker Compose | DevOps | PostgreSQL local | Onboarding nhanh | Có | Không dùng password demo production. |

## 18. Các quyết định thiết kế quan trọng

| Quyết định | Vấn đề giải quyết | Lợi ích | Trade-off | Khi áp dụng lại |
|---|---|---|---|---|
| Backend là source of truth | Client không được quyết định nghiệp vụ nhạy cảm | Giảm gian lận, đồng bộ Android/Admin | Backend phức tạp hơn | Nên áp dụng cho booking/payment/resource locks. |
| Không dùng `/api` prefix | Route ngắn, thống nhất hiện trạng | Client config đơn giản | Dễ nhầm nếu deploy sau dùng reverse proxy | Áp dụng nếu docs/client đều nhất quán. |
| Không gửi `userId` từ client | Tránh giả mạo ownership | JWT xác định user | Cần guard mọi protected API | Luôn áp dụng cho customer APIs. |
| Seat lock TTL 3 phút | Giữ ghế tạm thời | Giảm giữ ghế quá lâu | User phải thanh toán nhanh | Dùng cho tài nguyên khan hiếm. |
| Mock payment trước, provider thật sau | Demo core flow sớm | Test booking/ticket không phụ thuộc provider | Dễ để lại demo endpoint production | Giữ mock sau guard/dev mode. |
| SePay webhook xác nhận payment | Client không tự paid | Đúng mô hình payment thật | Cần public domain/API key/reconciliation | Bắt buộc cho real payment. |
| Polling trước WebSocket | Đơn giản hóa realtime | Ít hạ tầng hơn | Trễ 5 giây, tải polling | Hợp lý cho MVP/admin monitor. |
| Admin Web giảm tải | Tập trung vận hành demo | Hoàn thành nhanh | Thiếu auth/room CRUD | Tốt cho MVP, cần hardening sau. |
| Android dùng backend id String/UUID | Đồng bộ booking flow | Không cần mapping TMDB | Phụ thuộc backend data | Nên áp dụng khi backend có showtimes. |
| UI kit trước khi mở rộng screen | Tránh UI lệch | Tăng tốc screen sau | Cần discipline component | Nên áp dụng từ phase 1-2. |

## 19. Các lỗi/rủi ro/điểm yếu hiện tại

- Security: admin routes public; ticket verify/check-in public; `GET /payments/:id/status` public; CORS `origin: true`; dev JWT secret trong `api-server/.env.example`.
- Data consistency: chưa có DB-level constraint chống double-sell theo showtime/seat; race condition có thể xảy ra dưới concurrency cao.
- Payment: mock endpoint có thể nguy hiểm nếu bật production; SePay chưa có reconciliation, replay handling nâng cao, payment log dashboard; SePay webhook chưa thấy kiểm tra lock còn hạn như mock payment.
- Seat locking race condition: kiểm tra active lock trong transaction nhưng không có unique partial index cho active lock.
- Admin authorization: schema `Admin/AdminRole` có nhưng chưa gắn guard/login vào admin routes.
- Android state/navigation: payment cancellation release fallback nuốt lỗi release lock; base URL hardcoded public domain trong source hiện tại.
- Error handling: backend dùng exception cơ bản, chưa thấy global error format/filter riêng ngoài Nest default; client parse lỗi nhưng UX có thể còn thô.
- Environment/config: repo có `.env` local trong workspace nhưng tài liệu yêu cầu không commit secret; không đọc nội dung secret thật trong tài liệu này.
- Demo-only assumptions: seeded rooms/seats, public admin, mock revenue, QR string ticket.
- Production readiness: các phần cần bổ sung gồm auth admin, rate limit, audit log, observability, migration/reconciliation discipline, WebSocket/event stream nếu seat realtime là yêu cầu.

## 20. Bài học rút ra cho project cá nhân tiếp theo

1. Thiết kế backend là source of truth cho các trạng thái có tiền/tài nguyên/quyền truy cập.
2. Làm core flow end-to-end trước: auth -> resource selection -> lock -> booking -> payment -> ticket.
3. Làm auth sớm để API contract không phải sửa lại từ `userId` client sang JWT về sau.
4. Tách API layer/repository/UI state trên client để đổi backend hoặc sửa DTO không làm vỡ UI.
5. Không để client quyết định nghiệp vụ nhạy cảm như paid, sold, checked-in.
6. Payment thật phải đi qua backend/webhook; app chỉ tạo payment và polling/trình bày trạng thái.
7. Lock tài nguyên cần TTL, cleanup, release, chuyển trạng thái khi convert sang booking, và test cạnh tranh.
8. UI kit giúp tăng tốc phase sau, đặc biệt với status chip, state view, form, seat, ticket, payment.
9. Admin Web nên phục vụ demo/operation trước, tránh ôm CRUD phức tạp khi core flow chưa ổn.
10. Viết docs/checklist song song với code để tránh docs cũ mâu thuẫn source mới.
11. Build/test sau từng phase; smoke test API end-to-end rất đáng giữ.

## 21. Blueprint đề xuất cho project cá nhân mới

Phase 0 - Foundation:
- Tạo monorepo rõ `api-server`, `client`, `admin`, `docs`.
- Chốt entity/schema, enum/status, `api-server/.env.example`, docker-compose DB.
- Viết README setup, demo flow và checklist build/test ngay từ đầu.

Phase 1 - Auth & API skeleton:
- Local auth/JWT, optional social login.
- Guard/decorator current user.
- Validation pipe, error convention, smoke test health/auth.
- Quy tắc bắt buộc: protected customer API không nhận `userId` từ body/query.

Phase 2 - Core domain:
- CRUD/read model tối thiểu cho resource chính.
- Seed data đủ demo.
- Client lấy dữ liệu từ backend id chính, không dùng id ngoài nếu không có mapping.

Phase 3 - Resource locking/booking:
- Lock TTL, cleanup, create booking từ lock, cancel/release.
- Thêm transaction và constraint DB nếu tài nguyên có cạnh tranh.
- Viết test cho hai user cùng lock/pay một tài nguyên.

Phase 4 - Payment/ticket:
- Mock payment dev-only.
- Provider thật qua backend create + webhook + polling.
- Ticket/QR/check-in tách staff/admin.
- Thêm idempotency/reconciliation tối thiểu trước khi dùng tiền thật.

Phase 5 - Client UX:
- API client, token storage, repository, ViewModel/StateFlow hoặc query layer.
- UI kit/status/loading/error/empty.
- End-to-end customer flow.

Phase 6 - Admin/operations:
- Dashboard, monitor, verify/check-in, các CRUD thật sự cần.
- Admin auth/role trước khi deploy public.
- Giữ room/seat layout read-only nếu CRUD chưa cần cho demo.

Phase 7 - Production hardening:
- Rate limit, audit log, observability, retry/reconciliation, backup, CI build/test, staging/prod env.

## 22. Source Evidence Map

| Nhận định | Bằng chứng trong repo | Ghi chú |
|---|---|---|
| Repo có backend, Android, Admin Web | `README.md`, thư mục `api-server/`, `client-app/`, `admin-web/` | Monorepo rõ. |
| Backend không dùng `/api` prefix | `README.md`, `api-server/src/main.ts`, controllers dùng route trực tiếp | Không có `setGlobalPrefix`. |
| Backend dùng NestJS/Prisma/PostgreSQL | `api-server/package.json`, `api-server/prisma/schema.prisma`, `api-server/docker-compose.yml` | PostgreSQL 16 alpine local. |
| Validation whitelist/forbid unknown | `api-server/src/main.ts` | Global `ValidationPipe`. |
| Customer API lấy user từ JWT | `auth/jwt-auth.guard.ts`, `current-user.decorator.ts`, `bookings.controller.ts`, `payments.controller.ts` | Không nhận `userId` trong DTO chính. |
| Google login qua Firebase Admin | `auth/auth.service.ts`, `firebase/firebase-admin.service.ts`, `AUTH_TEST.md` | Firebase token đổi sang backend JWT. |
| Seat lock TTL 3 phút | `seat-locks/seat-locks.service.ts` | `LOCK_DURATION_MINUTES = 3`. |
| Seat map status tính theo showtime | `seats/seats.service.ts`, `api-server/README.md` | Sold/locked derived. |
| Booking tạo từ lockIds | `bookings/dto/create-booking.dto.ts`, `bookings.service.ts` | Không tạo từ seatIds trực tiếp. |
| Mock payment tạo payment/ticket | `payments/payments.service.ts` | Upsert payment/ticket, booking `PAID`. |
| SePay create/webhook/status có trong code | `payments.controller.ts`, `payments.service.ts`, `docs/sepay-payment-backend-summary.md` | Có API key webhook. |
| SePay webhook chưa kiểm tra lock expiry như mock payment | `api-server/src/payments/payments.service.ts` | `mockSuccess` kiểm tra active locks; `handleSepayWebhook` kiểm tra amount/booking/paid seats nhưng không thấy `lockedUntil`. |
| Ticket verify/check-in flow | `tickets.controller.ts`, `tickets.service.ts` | Check-in chuyển ticket `USED`, booking `CHECKED_IN`. |
| Admin routes public demo | `README.md`, controllers admin không có `UseGuards` | Cần production hardening. |
| Android dùng DataStore JWT | `client-app/.../core/storage/TokenManager.kt`, `AuthInterceptor.kt` | Bearer token tự attach. |
| Android BackendApi không có `/api` | `client-app/.../data/remote/api/BackendApi.kt` | Retrofit endpoints trực tiếp. |
| Android payment có mock và SePay | `PaymentRepositoryImpl.kt`, `PaymentViewModel.kt`, `PaymentScreen.kt` | SePay QR + polling. |
| Android ticket screen hiển thị QR string | `client-app/app/src/main/java/com/uit/eousx/presentation/ticket/TicketScreen.kt` | Chưa thấy QR bitmap rendering cho ticket. |
| Android booking history có tab trong Home | `HomeScreen.kt`, `BookingHistoryScreen.kt`, `BookingHistoryViewModel.kt` | Tab 1 trong Home mở booking history. |
| Android seat map polling 5s | `SeatMapViewModel.kt` | `POLLING_INTERVAL_MS = 5_000L`. |
| AI chat dùng Gemini và sanitize recommendation | `api-server/src/ai/ai.service.ts`, `api-server/src/ai/ai.controller.ts` | Chỉ recommend movie trong `AVAILABLE_MOVIES`. |
| Admin Web uses Axios/TanStack Query | `admin-web/package.json`, `src/shared/api/apiClient.ts`, feature pages | API fallback localhost. |
| Admin seat monitor polling 5s | `admin-web/src/features/seat-monitor/SeatMonitorPage.tsx` | `refetchInterval: 5000`. |
| Smoke test covers E2E API | `api-server/scripts/api-smoke-test.js`, `AUTH_TEST.md` | Writes `api-smoke-report.json`. |
| Seed data includes cinema/rooms/seats/movies/bookings | `api-server/prisma/seed.ts` | 124 seats logged. |

## 23. Những điểm chưa xác minh được

- Chưa xác minh được admin authentication/authorization đã được implement trong source hiện tại; controllers admin không có guard.
- Chưa xác minh được WebSocket realtime seat updates; source/docs hiện chỉ cho thấy polling.
- Chưa xác minh được production SePay mode, reconciliation job, payment log dashboard.
- Chưa xác minh được QR bitmap rendering đầy đủ cho ticket screen; docs component nói hiện QR string là MVP.
- Chưa xác minh được CI pipeline hoặc GitHub Actions trong repo hiện tại.
- Chưa xác minh được markdown formatter command có sẵn trong repo.
- Chưa xác minh được unit/integration tests backend ngoài smoke test script.
- Chưa xác minh được Room local database được dùng thật trong Android source; dependency có trong Gradle nhưng source hiện tại không cho thấy DAO/database.
- Chưa xác minh được admin login sử dụng model `Admin`; schema/seed có `Admin`, nhưng route guard/login admin chưa thấy.
- Chưa xác minh được production deployment config đầy đủ ngoài docs public domain/tunnel cho SePay.
