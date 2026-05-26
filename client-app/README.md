# EousX - Smart Movie Booking AI Platform 🎬✨

**EousX** là ứng dụng đặt vé xem phim thông minh, tích hợp trí tuệ nhân tạo (AI) để cá nhân hóa trải nghiệm người dùng, từ tư vấn chọn phim dựa trên tâm trạng đến quy trình đặt vé và thanh toán thời gian thực.

## 🏫 Thông tin đồ án
- **Trường:** Đại học Công nghệ thông tin - ĐHQG TP.HCM (UIT).
- **Khoa:** Mạng máy tính và Truyền thông dữ liệu.
- **Sinh viên thực hiện:**
    1. **Hoàng Xuân Đồng** - MSSV: 23520297
    2. **Đỗ Thái Hậu** - MSSV: 23520450

## 🚀 Tính năng cốt lõi (Must-have)
- **Xác thực & Quản lý:** Đăng ký/Đăng nhập (Email, Google, Facebook) và quản lý hồ sơ cá nhân.
- **Khám phá Điện ảnh:** Browse danh sách phim từ TMDB API, xem trailer YouTube và thông tin chi tiết (rating, diễn viên, thể loại).
- **Đặt vé thông minh:** Chọn rạp, suất chiếu theo thời gian thực và bản đồ ghế ngồi tương tác (Seat Map).
- **Thanh toán thực tế:** Tích hợp SDK chính thức của MoMo, ZaloPay hoặc VNPAY.
- **Vé điện tử:** Hiển thị mã QR để quét tại rạp và lưu lịch sử đặt vé.

## 🧠 Tính năng AI (Highlight)
- **AI Chatbot (Gemini 2.5 Flash):** Tư vấn phim tự nhiên theo yêu cầu ("Phim hành động hài cho nhóm bạn") hoặc theo tâm trạng (Mood/Emotion).
- **Cá nhân hóa:** Gợi ý phim dựa trên lịch sử xem và sở thích riêng của từng user.
- **Chat-to-Ticket:** Hỗ trợ luồng đặt vé trực tiếp thông qua hội thoại với AI.

## 🛠 Công nghệ & Kiến trúc
- **Frontend:** Kotlin, Jetpack Compose, Material Design 3.
- **Kiến trúc:** MVVM (Model-View-ViewModel) + Hilt (Dependency Injection).
- **Local DB:** Room Database (Hỗ trợ Offline-first).
- **Backend:** Node.js (Express) hoặc Python (FastAPI).
- **Realtime:** Firebase Firestore cho việc khóa ghế (Seat Locking) và Push Notification (FCM).

## 📋 Hướng dẫn cài đặt (Dành cho Developer)
1. **Clone project:** `git clone https://github.com/hoangdonguit/EousX-Movie-Booking-AI.git`
2. **Mở bằng Android Studio:** Đợi Gradle đồng bộ các dependencies.
3. **Cấu hình API Key:** Thêm API Key của TMDB và Google Gemini vào file `local.properties`.
4. **Run:** Chạy ứng dụng trên máy ảo hoặc thiết bị Android thật (API 24+).

---
*Dự án được thực hiện nhằm mục đích học tập và nghiên cứu công nghệ mới trong lĩnh vực di động và AI.*
