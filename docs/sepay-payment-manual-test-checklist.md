# EousX SePay Payment Manual Test Checklist

## 1. Start backend

Chạy backend local ở port `3000`.

```bash
cd api-server
npm.cmd run start:dev
```

Xác nhận backend chạy tại:

```text
http://localhost:3000
```

## 2. Check Cloudflare Tunnel

Đảm bảo public domain đang trỏ về backend local:

```text
https://api-eousx.hius.io.vn
```

Kiểm tra endpoint public không dùng prefix `/api`.

## 3. Login lấy JWT

Login bằng API auth hiện có để lấy JWT cho user test.

Lưu lại:

```text
Authorization: Bearer <JWT>
```

## 4. Get movies

Gọi API danh sách phim để chọn movie test.

Ghi lại:

```text
movieId
```

## 5. Get showtimes

Gọi API showtimes theo movie hoặc danh sách showtimes hiện có.

Ghi lại:

```text
showtimeId
```

## 6. Get seats

Gọi API lấy danh sách ghế theo showtime.

Chọn ghế còn available.

Ghi lại:

```text
seatId
```

## 7. Lock seats

Gọi API lock seat bằng JWT.

Ghi lại:

```text
lockIds
```

## 8. Create booking

Gọi API tạo booking từ `showtimeId` và `lockIds`.

Kỳ vọng:

```text
booking.status = WAITING_PAYMENT
```

Ghi lại:

```text
bookingId
booking.totalAmount
booking.code
```

## 9. Create SePay payment

Gọi:

```http
POST https://api-eousx.hius.io.vn/payments/sepay/create
Authorization: Bearer <JWT>
Content-Type: application/json
```

Body:

```json
{
  "bookingId": "<bookingId>"
}
```

Kỳ vọng:

```text
provider = SEPAY
paymentStatus = PENDING
bookingStatus = WAITING_PAYMENT
transactionId/paymentCode có dạng EOUSXYYYYMMDDXXXXXX
qrImageUrl có des=paymentCode
```

Ghi lại:

```text
paymentCode
qrImageUrl
amount
```

## 10. Check status before simulate

Gọi:

```http
GET https://api-eousx.hius.io.vn/payments/sepay/status/<paymentCode>
Authorization: Bearer <JWT>
```

Kỳ vọng:

```json
{
  "paymentCode": "<paymentCode>",
  "paymentStatus": "PENDING",
  "bookingStatus": "WAITING_PAYMENT",
  "bookingId": "<bookingId>",
  "hasTicket": false
}
```

## 11. Simulate transaction in SePay Test Mode

Trong SePay Test Mode, simulate giao dịch incoming.

Nội dung giao dịch phải chứa:

```text
<paymentCode>
```

Số tiền phải bằng:

```text
booking.totalAmount
```

`transferType` phải là:

```text
in
```

## 12. Check backend log

Kiểm tra backend log có dòng nhận webhook:

```text
[SePay Webhook] received payload:
```

Payload phải có:

```text
transferType = in
transferAmount = booking.totalAmount
content hoặc description chứa paymentCode
```

## 13. Check payment status after webhook

Gọi lại:

```http
GET https://api-eousx.hius.io.vn/payments/sepay/status/<paymentCode>
Authorization: Bearer <JWT>
```

Kỳ vọng:

```json
{
  "paymentCode": "<paymentCode>",
  "paymentStatus": "SUCCESS",
  "bookingStatus": "PAID",
  "bookingId": "<bookingId>",
  "hasTicket": true
}
```

## 14. Check ticket

Gọi API booking/payment/ticket hiện có để xác nhận:

```text
Ticket tồn tại
ticket.status = VALID
ticket.bookingId = bookingId
```

## 15. Check seat SOLD

Kiểm tra lại seat map cho showtime.

Kỳ vọng ghế đã thanh toán không còn available cho user khác.

Trạng thái liên quan:

```text
Booking = PAID
SeatLock = CONVERTED_TO_BOOKING
Ticket = VALID
```

## 16. Test duplicate webhook

Gửi lại cùng webhook hoặc simulate lại cùng nội dung nếu SePay Test Mode cho phép.

Kỳ vọng:

```text
Không tạo duplicate ticket
Payment vẫn SUCCESS
Booking vẫn PAID
Response vẫn success true
```

## 17. Test wrong amount

Tạo booking/payment mới.

Simulate giao dịch có đúng `paymentCode` nhưng sai `transferAmount`.

Kỳ vọng:

```text
Webhook bị ignored
Payment vẫn PENDING
Booking vẫn WAITING_PAYMENT
Ticket chưa được tạo
Response success true
```

## 18. Test wrong paymentCode

Simulate giao dịch có amount đúng nhưng `paymentCode` không tồn tại.

Kỳ vọng:

```text
Webhook bị ignored
Không payment nào bị cập nhật
Không booking nào chuyển PAID
Response success true
```

## 19. Test transferType out

Simulate hoặc gửi payload có:

```text
transferType = out
```

Kỳ vọng:

```text
Webhook bị ignored
Không payment nào bị cập nhật
Không booking nào chuyển PAID
Response success true
```

## 20. Demo script

Kịch bản demo ngắn:

1. User đăng nhập và lấy JWT.
2. User chọn phim, suất chiếu, ghế.
3. User lock seat và tạo booking.
4. User tạo SePay payment.
5. App hiển thị QR có `paymentCode`.
6. SePay Test Mode gửi webhook incoming đúng amount và đúng `paymentCode`.
7. Backend xác nhận payment.
8. App polling status thấy `SUCCESS`, `PAID`, `hasTicket = true`.
9. User mở ticket.
10. Nếu SePay không sẵn sàng, dùng Mock Payment làm fallback demo.
