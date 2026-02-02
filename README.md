# VNPay Spring Boot Demo

## 📌 Mô tả

Dự án demo tích hợp **cổng thanh toán VNPay** vào ứng dụng **Spring Boot**, phục vụ mục đích học tập và tham khảo.

---

## ✅ Chức năng hiện có

* Tạo yêu cầu thanh toán VNPay từ backend Spring Boot
* Redirect người dùng sang cổng thanh toán VNPay (Sandbox)
* Nhận kết quả thanh toán qua **Return URL**
* Xác thực checksum (hash) từ VNPay
* Nhận IPN và cập nhật trạng thái giao dịch vào DB
* QueryDR và Refund qua API VNPAY
* Demo giao diện thanh toán đơn giản

---

## 🛠 Công nghệ sử dụng

* Java 17
* Spring Boot 3
* Maven
* VNPay Payment Gateway
* Thymeleaf + Bootstrap (demo)
* PostgreSQL + Spring Data JPA

---

## ▶️ Chạy dự án

### 1) Chuẩn bị biến môi trường

Thiết lập các biến sau trước khi chạy:

```
VNPAY_TMN_CODE=...
VNPAY_SECRET_KEY=...
VNPAY_RETURN_URL=http://localhost:9999/vnpay/vnpay-return
DB_USERNAME=...
DB_PASSWORD=...
```

Tùy chọn:

```
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_API_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
DB_URL=jdbc:postgresql://localhost:5432/vnpay_pg_db
PORT=9999
```

### Chạy trên IntelliJ IDEA

1. Mở project bằng **IntelliJ IDEA**
2. Đợi IntelliJ load Maven dependencies
3. Mở file `Application.java`
4. Chọn **Run ▶️** (hoặc `Shift + F10`)

Ứng dụng chạy tại: `http://localhost:9999`

---

## ⚠️ Lưu ý

* Chỉ phù hợp cho **demo / học tập**
* Chưa có test tự động
* Cần bổ sung monitoring, audit và bảo mật production nếu triển khai thực tế

---

## 👨‍💻 Tác giả
SangLV
