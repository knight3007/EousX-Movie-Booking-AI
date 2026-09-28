# EousX SePay Payment Backend Summary

## 1. Mục tiêu tích hợp

Tích hợp SePay Test Mode cho backend EousX để backend tự xác nhận thanh toán sau khi nhận webhook từ SePay. Android chỉ tạo thanh toán và polling trạng thái, không tự xác nhận thanh toán, không gọi webhook, và không lưu SePay API Key.

Mock Payment vẫn được giữ làm phương án fallback cho demo hoặc khi cần kiểm thử nhanh.

## 2. Trạng thái hoàn thành

- Đã có `POST /payments/sepay/create` để tạo thanh toán SePay.
- Đã có `POST /payments/webhook/sepay` để nhận webhook từ SePay.
- Đã có xác thực SePay webhook bằng API Key.
- Backend đã nhận được payload thật từ SePay Test Mode.
- Webhook đã xử lý xác nhận payment hợp lệ.
- Đã có `GET /payments/sepay/status/:paymentCode` cho Android polling trạng thái bằng JWT.
- Backend không dùng prefix `/api`.

## 3. Kiến trúc tổng quan

Luồng chính:

1. Android đăng nhập và có JWT.
2. Android tạo booking ở trạng thái `WAITING_PAYMENT`.
3. Android gọi `POST /payments/sepay/create`.
4. Backend tạo `Payment` với `provider = SEPAY`, `status = PENDING`, `transactionId = paymentCode`.
5. Backend trả `qrImageUrl` có tham số `des = paymentCode`.
6. Người dùng thanh toán hoặc mô phỏng giao dịch trong SePay Test Mode.
7. SePay gọi webhook về backend.
8. Backend xác minh API Key, match `paymentCode`, kiểm tra số tiền, rồi xác nhận thanh toán.
9. Android polling `GET /payments/sepay/status/:paymentCode` để biết booking đã paid và ticket đã có chưa.

## 4. Cloudflare Tunnel và public endpoint

Public domain:

```text
https://api-eousx.hius.io.vn
```

Domain này trỏ về backend local:

```text
http://localhost:3000
```

thông qua Cloudflare Tunnel.

Webhook URL cấu hình trong SePay:

```text
https://api-eousx.hius.io.vn/payments/webhook/sepay
```

Lưu ý backend không có prefix `/api`, vì vậy không dùng `/api/payments/...`.

## 5. Biến môi trường cần có

Các biến môi trường SePay cần có ở backend:

```env
SEPAY_WEBHOOK_API_KEY="replace_with_sepay_webhook_api_key"
SEPAY_QR_BASE_URL="https://qr.sepay.vn/img"
SEPAY_BANK_CODE="replace_with_bank_code"
SEPAY_BANK_ACCOUNT_NUMBER="replace_with_bank_account_number"
```

Không commit secret thật. Chỉ commit placeholder trong `.env.example`.

## 6. API đã thêm

### Tạo thanh toán SePay

```http
POST /payments/sepay/create
Authorization: Bearer <JWT>
Content-Type: application/json
```

Body:

```json
{
  "bookingId": "<bookingId>"
}
```

Kết quả chính:

- Tạo hoặc cập nhật `Payment`.
- `provider = SEPAY`.
- `status = PENDING`.
- `transactionId = paymentCode`.
- Trả `qrImageUrl` có `des = paymentCode`.

### Webhook SePay

```http
POST /payments/webhook/sepay
```

Route public nhưng được bảo vệ bằng SePay API Key.

### Polling trạng thái SePay

```http
GET /payments/sepay/status/:paymentCode
Authorization: Bearer <JWT>
```

Response:

```json
{
  "paymentCode": "EOUSX202605280OGK4S",
  "paymentStatus": "PENDING",
  "bookingStatus": "WAITING_PAYMENT",
  "bookingId": "<bookingId>",
  "hasTicket": false
}
```

## 7. Payload webhook thật từ SePay Test Mode

Ví dụ payload thật đã nhận:

```json
{
  "gateway": "Vietcombank",
  "transactionDate": "2026-05-28 21:36:37",
  "accountNumber": "0000000001",
  "subAccount": "SBSEPAYPT1HZ9RSYAKF",
  "code": "",
  "content": "Giao dich thu nghiem ...",
  "transferType": "in",
  "description": "Giao dich thu nghiem ...",
  "transferAmount": 100000,
  "referenceCode": "SB52117364BC30",
  "accumulated": 0,
  "id": 4795
}
```

Quan trọng: field `code` có thể rỗng trong Test Mode, nên backend không dựa vào `code`.

## 8. Cách backend match paymentCode

Backend lấy `paymentCode` từ `content` hoặc `description` của webhook.

Format payment code hiện tại:

```text
EOUSXYYYYMMDDXXXXXX
```

Ví dụ:

```text
EOUSX202605280OGK4S
```

Sau khi extract được code, backend tìm `Payment` theo:

- `provider = "SEPAY"`
- `transactionId = paymentCode`

## 9. Luồng xử lý webhook

Backend xử lý webhook theo thứ tự:

1. Xác thực SePay API Key ở controller.
2. Log payload nhận được.
3. Chỉ xử lý `transferType = "in"`.
4. Extract `paymentCode` từ `content` hoặc `description`.
5. Tìm `Payment` có `provider = SEPAY` và `transactionId = paymentCode`.
6. Nếu không tìm thấy payment thì trả `{ "success": true }` để SePay không retry vô ích.
7. Kiểm tra `transferAmount` bằng `booking.totalAmount`.
8. Nếu sai số tiền thì bỏ qua và không cập nhật payment, booking, ticket.
9. Nếu payment đã `SUCCESS` và booking đã `PAID` hoặc `CHECKED_IN`, bỏ qua để tránh tạo ticket trùng.
10. Nếu booking không còn `WAITING_PAYMENT`, bỏ qua.
11. Kiểm tra ghế không bị booking khác đã `PAID` hoặc `CHECKED_IN` giữ.
12. Cập nhật `Payment.status = SUCCESS`, `paidAt = now`, `amount = booking.totalAmount`.
13. Cập nhật `Booking.status = PAID`.
14. Upsert `Ticket` theo `bookingId`, status `VALID`.
15. Chuyển các active `SeatLock` liên quan sang `CONVERTED_TO_BOOKING`.

## 10. Quy tắc bảo mật

- Webhook public nhưng phải có SePay API Key hợp lệ.
- Không nhận `userId` từ webhook.
- Ownership chỉ kiểm tra ở API dành cho Android polling bằng JWT.
- Android không được biết SePay API Key.
- Không commit secret thật vào git.
- Backend là nơi duy nhất xác nhận thanh toán thật.

## 11. Quy tắc Android

- Android gọi `POST /payments/sepay/create` để lấy `paymentCode` và `qrImageUrl`.
- Android hiển thị QR cho người dùng.
- Android polling `GET /payments/sepay/status/:paymentCode` bằng JWT.
- Android không tự đổi booking sang paid.
- Android không gọi `POST /payments/webhook/sepay`.
- Android không lưu SePay API Key.
- Mock Payment vẫn có thể dùng làm fallback khi cần.

## 12. Những lỗi đã phòng tránh

- Không dùng `/api` prefix sai với backend hiện tại.
- Không dựa vào `body.code` vì payload thật có thể để `code = ""`.
- Không xác nhận giao dịch `transferType = "out"`.
- Không xác nhận nếu sai số tiền.
- Không nhận `userId` từ webhook.
- Không tạo duplicate ticket khi webhook gửi lại nhiều lần.
- Không bán ghế nếu ghế đã thuộc booking khác `PAID` hoặc `CHECKED_IN`.
- Không để Android tự confirm payment.

## 13. Phạm vi chưa làm

- Chưa tích hợp SePay Production Mode.
- Chưa có reconciliation job để đối soát giao dịch định kỳ.
- Chưa có dashboard admin riêng cho payment logs.
- Chưa có retry nội bộ nếu database lỗi trong lúc xử lý webhook.
- Chưa thay Mock Payment bằng real payment hoàn toàn.

## 14. Bước tiếp theo

- Chạy checklist manual với Postman và SePay Test Mode.
- Cập nhật Android để dùng `POST /payments/sepay/create` và polling `GET /payments/sepay/status/:paymentCode`.
- Giữ Mock Payment làm fallback trong demo.
- Khi chuyển production, cấu hình lại biến môi trường, webhook URL, tài khoản ngân hàng, và kiểm tra bảo mật API Key.
