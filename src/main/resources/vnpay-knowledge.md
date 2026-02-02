# Tài liệu đặc tả kết nối VNPAY — Techspec Post method 2.1.0

Nguồn: [VNPAY Payment Gateway Techspec Post method 2.1.0-VN](https://sandbox.vnpayment.vn/apis/files/VNPAY%20Payment%20Gateway_Techspec%20Post%20method%202.1.0-VN.pdf)

---

## 1. Thuật ngữ

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| TMĐT | Thương mại điện tử |
| VnPayment | Cổng thanh toán VNPAY |
| Merchant | Đơn vị chấp nhận thẻ |
| API | Giao diện kết nối để merchant tương tác với hệ thống VNPAY |
| Checksum | Mã kiểm tra sự toàn vẹn của dữ liệu (phiên bản 2.1.0 mặc định HMACSHA512) |
| OTP | Mật khẩu xác thực một lần cho giao dịch |

## 2. Kiểu dữ liệu

- **Alpha**: Chuỗi A-Z, a-z
- **Numeric**: Chỉ số 0-9
- **Alphanumeric**: Chữ và số

## 3. Quy trình thanh toán

- Khách chọn Thanh toán qua VNPAY trên website TMĐT.
- Website gửi thông tin thanh toán sang VNPAY (popup/iframe) theo đặc tả.
- Khách nhập thông tin xác thực tại VNPAY; VNPAY gửi OTP từ Ngân hàng.
- Khách nhập OTP; xác thực thành công thì Ngân hàng chuẩn chi.
- VNPAY gửi kết quả cho website TMĐT qua **IPN URL** và chuyển khách tới **Return URL**.

**Xử lý đơn hàng:** Thành công → giao hàng/cung cấp dịch vụ. Thành công nhưng hết hàng → hoàn tiền qua hệ thống VNPAY. Không thành công → hiển thị mã lỗi cho khách.

## 4. Yêu cầu thanh toán (vnp_Command=pay) — Merchant gửi VNPAY

| Tham số | Bắt buộc | Kiểu | Mô tả / Ví dụ |
|---------|----------|------|----------------|
| vnp_Version | Có | Alphanumeric 1-8 | Phiên bản API: **2.1.0** |
| vnp_Command | Có | Alpha 1-16 | **pay** |
| vnp_TmnCode | Có | Alphanumeric 8 | Mã merchant trên VNPAY |
| vnp_BankCode | Không | Alphanumeric 3-20 | Mã ngân hàng (VD: VIETCOMBANK). Không truyền = khách chọn tại VNPAY |
| vnp_Locale | Có | Alpha 2-5 | Ngôn ngữ: **vn** hoặc **en** |
| vnp_CurrCode | Có | Alpha 3 | **VND** hoặc **USD** |
| vnp_TxnRef | Có | Alphanumeric 1-100 | Mã tham chiếu giao dịch tại merchant, **không trùng trong ngày** |
| vnp_OrderInfo | Có | Alphanumeric 1-255 | Mô tả thanh toán (Tiếng Việt không dấu) |
| vnp_OrderType | Có | Alphanumeric 1-100 | Mã danh mục hàng hóa (VD: topup) |
| vnp_Amount | Có | Numeric 1-12 | Số tiền: **nhân 100** (VD: 10.000 VND → 1000000) |
| vnp_ReturnUrl | Có | Alphanumeric 10-255 | URL redirect sau khi thanh toán xong |
| vnp_IpAddr | Có | Alphanumeric 7-45 | IP khách hàng |
| vnp_CreateDate | Có | Numeric 14 | Thời gian tạo GD **GMT+7**, format **yyyyMMddHHmmss** |
| vnp_ExpireDate | Có | Numeric 14 | Thời gian hết hạn thanh toán, **yyyyMMddHHmmss** |
| vnp_Bill_Mobile, vnp_Bill_Email, vnp_Bill_FirstName, vnp_Bill_LastName, vnp_Bill_Address, vnp_Bill_City, vnp_Bill_Country, vnp_Bill_State | Không | — | Thông tin hóa đơn/khách hàng |
| vnp_Inv_* (Phone, Email, Customer, Address, Company, Taxcode, Type) | Không | — | Thông tin hóa đơn điện tử |
| **vnp_SecureHash** | Có | Alphanumeric 32-256 | Checksum HMACSHA512 (xem mục Checksum) |

## 5. Thông tin nhận về từ VNPAY (sau thanh toán / Return URL, IPN)

vnp_TmnCode, vnp_TxnRef, vnp_Amount, vnp_OrderInfo, **vnp_ResponseCode** (00 = thành công), vnp_BankCode, vnp_BankTranNo, vnp_CardType (ATM/IB/ACC/QRCODE), **vnp_PayDate** (yyyyMMddHHmmss), **vnp_TransactionNo** (mã GD tại VNPAY), **vnp_TransactionStatus** (00 = thành công), **vnp_SecureHash** (bắt buộc kiểm tra).

## 6. Truy vấn giao dịch (vnp_Command=querydr)

- **Method:** POST, Content-Type: application/json
- **Merchant gửi:** vnp_RequestId (unique/ngày), vnp_Version 2.1.0, vnp_Command=querydr, vnp_TmnCode, vnp_TxnRef, vnp_OrderInfo, vnp_TransactionNo (tùy chọn), vnp_TransactionDate (yyyyMMddHHmmss, tham chiếu vnp_CreateDate của pay), vnp_CreateDate, vnp_IpAddr, vnp_SecureHash.
- **Quy tắc checksum querydr:** data = vnp_RequestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" + vnp_TxnRef + "|" + vnp_TransactionDate + "|" + vnp_CreateDate + "|" + vnp_IpAddr + "|" + vnp_OrderInfo; checksum = HMACSHA512(secretKey, data).
- **VNPAY trả về:** vnp_ResponseId, vnp_Command, vnp_TmnCode, vnp_TxnRef, vnp_Amount, vnp_OrderInfo, vnp_ResponseCode (00 = thành công), vnp_Message, vnp_BankCode, vnp_CardType, vnp_PayDate, vnp_TransactionNo, vnp_TransactionType (01=thanh toán, 02=hoàn toàn phần, 03=hoàn một phần), vnp_TransactionStatus, vnp_PromotionCode, vnp_PromotionAmount, vnp_SecureHash.

## 7. Hoàn trả giao dịch (vnp_Command=refund)

- **Method:** POST, Content-Type: application/json
- Số tiền hoàn ≤ số tiền giao dịch thanh toán.
- **Merchant gửi:** vnp_RequestId, vnp_Version 2.1.0, vnp_Command=refund, vnp_TmnCode, **vnp_TransactionType** (02=hoàn toàn phần, 03=hoàn một phần), vnp_TxnRef, vnp_Amount (số tiền hoàn, nhân 100), vnp_OrderInfo, vnp_TransactionNo (tùy chọn), vnp_TransactionDate (yyyyMMddHHmmss), **vnp_CreateBy** (người tạo hoàn tiền), vnp_CreateDate, vnp_IpAddr, vnp_SecureHash.
- **Quy tắc checksum refund:** data = vnp_RequestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" + vnp_TransactionType + "|" + vnp_TxnRef + "|" + vnp_Amount + "|" + vnp_TransactionNo + "|" + vnp_TransactionDate + "|" + vnp_CreateBy + "|" + vnp_CreateDate + "|" + vnp_IpAddr + "|" + vnp_OrderInfo; checksum = HMACSHA512(secretKey, data).
- **VNPAY trả về:** vnp_ResponseId, vnp_Command, vnp_TmnCode, vnp_TxnRef, vnp_Amount, vnp_OrderInfo, vnp_ResponseCode (00=thành công), vnp_Message, vnp_BankCode, vnp_PayDate, vnp_TransactionNo, vnp_TransactionType, vnp_TransactionStatus, vnp_SecureHash.

## 8. Hoàn trả theo lô (vnp_Command=refundbatch)

- Tối đa 50 giao dịch/lô. Merchant gửi vnp_RequestId, vnp_Version, vnp_Command=refundbacth, vnp_TmnCode, vnp_CreateBy, vnp_CreateDate, vnp_IpAddr, **vnp_Data** (mảng JSON các item: vnp_DataId, vnp_TransactionType, vnp_TxnRef, vnp_Amount, vnp_OrderInfo, vnp_TransactionNo, vnp_TransDate), vnp_SecureHash. Checksum: data = vnp_RequestId + "|" + vnp_Version + "|" + ... + "|" + vnp_Data.

## 9. Checksum (vnp_SecureHash) — Phiên bản 2.1.0

- **Phương thức:** HMACSHA512 với Secret Key (hashSecret) của merchant.
- **Quy tắc chung (pay / URL redirect):** Sắp xếp các tham số theo **thứ tự alphabet** (A-Z), **bỏ** vnp_SecureHash và vnp_SecureHashType, nối key=value bằng **&**, value cần **URL-encode** khi build query; chuỗi hashData (trước khi hash) là các cặp key=value đã sắp xếp, nối & . **vnp_SecureHash = HMAC_SHA512(secretKey, hashData)**.
- Query DR và Refund có quy tắc nối chuỗi riêng (theo mục 6, 7): thứ tự field cố định, nối bằng "|".

## 10. Bảng mã lỗi — vnp_ResponseCode

- **00**: Thành công cho tất cả API.
- **IPN / Cập nhật kết quả:** 05 (không đủ số dư), 06 (sai OTP), 07 (trừ tiền thành công nhưng GD nghi ngờ), 09 (chưa đăng ký InternetBanking), 10 (xác thực sai quá 3 lần), 11 (hết hạn chờ thanh toán), 12 (thẻ/tài khoản bị khóa), 24 (khách hủy), 79 (sai mật khẩu quá số lần), 65 (vượt hạn mức), 75 (ngân hàng bảo trì), 99 (lỗi khác).
- **Merchant trả IPN cho VNPAY:** 00 (ghi nhận thành công), 01 (không tìm thấy đơn), 02 (đã xử lý trước đó), 03 (IP không được phép), 97 (sai chữ ký), 99 (lỗi hệ thống).
- **QueryDR:** 02 (merchant không hợp lệ), 03 (dữ liệu sai định dạng), 08 (bảo trì), 91 (không tìm thấy GD), 97 (chữ ký không hợp lệ), 99 (lỗi khác).
- **Refund:** 02, 03, 08, **16** (không hoàn tiền trong thời gian này), **91** (không tìm thấy GD), **93** (số tiền hoàn không hợp lệ), **94** (đã gửi yêu cầu hoàn trước đó), **95** (GD không thành công bên VNPAY), 97, 99.
- **Refundbatch:** 02, 03, 08, 94 (yêu cầu lô đã gửi trước đó), 97, 99; vnp_DataResponseCode: 16, 91, 93, 94, 95, 99.

## 11. Bảng mã trạng thái giao dịch — vnp_TransactionStatus

00=Thành công, 01=Chưa hoàn tất, 02=Lỗi, 04=Đảo (đã trừ tiền NH nhưng chưa thành công VNPAY), 05=VNPAY đang xử lý hoàn tiền, 06=VNPAY đã gửi hoàn tiền sang NH, 07=Nghi ngờ gian lận, 08=Quá thời gian thanh toán, 09=Hoàn trả bị từ chối, 10=Đã giao hàng, 11=Hủy, 20=Đã thanh quyết toán cho merchant.

## 12. Thư viện / Tạo checksum (tóm tắt)

- Sắp xếp tham số theo key A-Z, bỏ vnp_SecureHash, vnp_SecureHashType.
- Tạo chuỗi hashData: key1=value1&key2=value2&... (value URL-encode khi build URL).
- vnp_SecureHash = HMAC_SHA512(secretKey, hashData).
- Java: `Mac.getInstance("HmacSHA512")`, secret key bytes, hashData bytes → hex.
- PHP: `hash_hmac('sha512', $hashdata, $vnp_HashSecret)`.

---

*Tài liệu gốc: VNPAY Payment Gateway Techspec Post method 2.1.0-VN (PDF).*
