# Công cụ hỗ trợ CTT VNPAY

## 📌 Mô tả

**Công cụ hỗ trợ tích hợp Cổng thanh toán VNPAY (CTT VNPAY)** — ứng dụng web Spring Boot cung cấp:

* **Công cụ hỗ trợ kỹ thuật**: Encode/Decode URL, Check URL thanh toán, Tạo checksum (HMAC-SHA512 2.1.0)
* **Demo / Thử nghiệm**: Tạo đơn, truy vấn giao dịch, hoàn tiền (Luồng Payment)
* **Thống kê đơn hàng**: Dashboard và đơn hàng gần đây từ demo
* **Trợ lý chat**: Hỏi đáp về VNPAY — có thể gắn **Gemini** hoặc **ChatGPT** và cung cấp **tài liệu VNPAY** để trả lời theo ngữ cảnh

Phù hợp cho **developer** tích hợp VNPAY và cần kiểm tra chữ ký, URL, demo luồng thanh toán.

---

## ✅ Chức năng chính

### Công cụ hỗ trợ (Support)

| Công cụ | Mô tả |
|--------|--------|
| **Encode / Decode** | Mã hóa / giải mã chuỗi URL (UTF-8) |
| **Check URL thanh toán** | Nhập full URL + Secret Key → kiểm tra chữ ký, trả về URL đúng nếu sai |
| **Tạo checksum** | Tạo vnp_SecureHash (HMAC-SHA512, version 2.1.0) từ query string + Secret Key |

### Demo / Thử nghiệm (Luồng Payment)

* Tạo đơn hàng → redirect sang cổng VNPAY (Sandbox)
* Return URL, IPN, xác thực checksum
* QueryDR (truy vấn giao dịch), Refund (hoàn tiền)
* Thống kê đơn hàng và đơn gần đây

---

## 🛠 Công nghệ

* Java 17 · Spring Boot 3 · Maven
* Thymeleaf + Bootstrap 5 · Font Awesome
* PostgreSQL + Spring Data JPA
* VNPAY Payment Gateway (Sandbox)

---

## ▶️ Chạy dự án

### 1) Biến môi trường

Tạo file `.env` hoặc cấu hình Run trong IDE:

```env
VNPAY_TMN_CODE=...
VNPAY_SECRET_KEY=...
VNPAY_RETURN_URL=http://localhost:9999/vnpay_support/demo/vnpay-return
DB_USERNAME=...
DB_PASSWORD=...
```

Tùy chọn:

```env
PORT=9999
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_API_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
DB_URL=jdbc:postgresql://localhost:5432/vnpay_pg_db
```

**Chatbot (Gemini hoặc OpenAI):** Nếu cấu hình API key, trợ lý chat sẽ dùng LLM và tài liệu VNPAY thay vì rule-based.

```env
CHAT_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key
# Hoặc dùng OpenAI:
# CHAT_PROVIDER=openai
# OPENAI_API_KEY=your_openai_api_key
CHAT_KNOWLEDGE_PATH=classpath:vnpay-knowledge.md
```

Tài liệu VNPAY: file `src/main/resources/vnpay-knowledge.md` đã được điền sẵn nội dung tóm tắt từ **[VNPAY Payment Gateway Techspec Post method 2.1.0-VN](https://sandbox.vnpayment.vn/apis/files/VNPAY%20Payment%20Gateway_Techspec%20Post%20method%202.1.0-VN.pdf)** (thuật ngữ, quy trình thanh toán, tham số pay/querydr/refund, checksum HMACSHA512, mã lỗi, mã trạng thái). Bạn có thể chỉnh sửa hoặc bổ sung; chatbot sẽ dùng làm ngữ cảnh để trả lời theo tài liệu này.

#### Làm sao để chatbot thông minh như Gemini

1. **Lấy API key Gemini** (miễn phí): vào [Google AI Studio](https://aistudio.google.com/apikey) → Create API key.
2. **Thêm vào `.env`**:
   ```env
   CHAT_PROVIDER=gemini
   GEMINI_API_KEY=AIza...   # dán key vừa tạo
   ```
3. **Dùng model mạnh** (mặc định đã là `gemini-1.5-pro`): trả lời chi tiết, giải thích rõ. Nếu muốn nhanh hơn, tiết kiệm hơn thì đặt `CHAT_GEMINI_MODEL=gemini-1.5-flash`.
4. **Tài liệu VNPAY**: file `vnpay-knowledge.md` đã có sẵn nội dung từ techspec 2.1.0 (link PDF ở trên). Bạn có thể mở file đó để sửa/bổ sung; chatbot sẽ ưu tiên trả lời dựa trên nội dung này.
5. **Khởi động lại ứng dụng** rồi mở widget chat — trợ lý sẽ trả lời bằng Gemini, thông minh và bám sát tài liệu bạn cung cấp.

### 2) Chạy

* **Maven**: `./mvnw spring-boot:run`
* **IntelliJ**: Run `SpringbootApplication.java`

Ứng dụng: **http://localhost:9999/vnpay_support/**

---

## 📁 Cấu trúc URL

| Mục | URL |
|-----|-----|
| Trang chủ | `/vnpay_support/` |
| Công cụ hỗ trợ | `/vnpay_support/support` |
| Encode/Decode | `/vnpay_support/support/url-encode` |
| Check URL thanh toán | `/vnpay_support/support/check-payment-url` |
| Tạo checksum | `/vnpay_support/support/tao-checksum` |
| Demo / Thử nghiệm | `/vnpay_support/demo` |
| Thống kê đơn hàng | `/vnpay_support/demo/dashboard` |

---

## ⚠️ Lưu ý

* Dự án là **công cụ hỗ trợ / demo**, không thay thế tài liệu chính thức VNPAY.
* Cần bổ sung bảo mật, monitoring nếu dùng trong môi trường thật.

---

## 👨‍💻 Tác giả

SangLV
