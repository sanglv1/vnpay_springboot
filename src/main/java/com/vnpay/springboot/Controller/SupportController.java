package com.vnpay.springboot.Controller;

import com.vnpay.springboot.Util.VNPayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequestMapping("/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);

    // ------------------- LANDING -------------------
    @GetMapping({"", "/"})
    public String supportIndex() {
        return "support/index";
    }

    // ------------------- CÔNG CỤ KIỂM TRA CHỮ KÝ -------------------
    @GetMapping("/checksum")
    public String checksumForm() {
        return "support/checksum";
    }

    @PostMapping("/checksum")
    public String verifyChecksum(
            @RequestParam String queryString,
            @RequestParam String secretKey,
            Model model) {

        if (queryString == null || queryString.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập chuỗi query (từ Return URL hoặc tham số VNPAY).");
            model.addAttribute("queryString", queryString);
            model.addAttribute("secretKey", secretKey);
            return "support/checksum";
        }

        if (secretKey == null || secretKey.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập Secret Key.");
            model.addAttribute("queryString", queryString);
            model.addAttribute("secretKey", secretKey);
            return "support/checksum";
        }

        Map<String, String> params = parseQueryString(queryString.trim());
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
            model.addAttribute("error", "Chuỗi query không chứa vnp_SecureHash.");
            model.addAttribute("queryString", queryString);
            model.addAttribute("secretKey", secretKey);
            return "support/checksum";
        }

        String hashData = buildHashData(params);
        String computedHash = VNPayUtils.hmacSHA512(secretKey.trim(), hashData);
        boolean valid = computedHash.equalsIgnoreCase(vnpSecureHash);

        model.addAttribute("valid", valid);
        model.addAttribute("computedHash", computedHash);
        model.addAttribute("receivedHash", vnpSecureHash);
        model.addAttribute("hashData", hashData);
        model.addAttribute("queryString", queryString);
        model.addAttribute("secretKey", secretKey);
        return "support/checksum";
    }

    // ------------------- URL ENCODE / DECODE -------------------
    @GetMapping("/url-encode")
    public String urlEncodeForm() {
        return "support/url-encode";
    }

    @PostMapping("/url-encode")
    public String urlEncodeDecode(
            @RequestParam(required = false) String input,
            @RequestParam(defaultValue = "encode") String action,
            Model model) {
        model.addAttribute("input", input);
        model.addAttribute("action", action);
        if (input == null || input.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập nội dung.");
            return "support/url-encode";
        }
        try {
            if ("decode".equalsIgnoreCase(action)) {
                model.addAttribute("output", URLDecoder.decode(input.trim(), StandardCharsets.UTF_8.toString()));
            } else {
                model.addAttribute("output", URLEncoder.encode(input.trim(), StandardCharsets.UTF_8.toString()));
            }
        } catch (UnsupportedEncodingException e) {
            model.addAttribute("error", "Lỗi mã hóa: " + e.getMessage());
        }
        return "support/url-encode";
    }

    // ------------------- CHUẨN CHUỖI HASHDATA -------------------
    @GetMapping("/hashdata")
    public String hashDataForm() {
        return "support/hashdata";
    }

    @PostMapping("/hashdata")
    public String buildHashDataTool(
            @RequestParam(required = false) String queryString,
            @RequestParam(required = false) Boolean excludeSecureHash,
            Model model) {
        model.addAttribute("queryString", queryString);
        model.addAttribute("excludeSecureHash", excludeSecureHash != null && excludeSecureHash);
        if (queryString == null || queryString.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập chuỗi query (key1=value1&key2=value2).");
            return "support/hashdata";
        }
        Map<String, String> params = parseQueryString(queryString.trim());
        boolean exclude = excludeSecureHash != null && excludeSecureHash;
        String hashData = buildHashData(params, exclude);
        model.addAttribute("hashData", hashData);
        model.addAttribute("paramCount", params.size());
        return "support/hashdata";
    }

    // ------------------- VALIDATE DỮ LIỆU GỬI VNPAY -------------------
    @GetMapping("/validate")
    public String validateForm() {
        return "support/validate";
    }

    @PostMapping("/validate")
    public String validateData(
            @RequestParam(required = false) String queryString,
            Model model) {
        model.addAttribute("queryString", queryString);
        if (queryString == null || queryString.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập chuỗi query hoặc tham số gửi sang VNPAY.");
            return "support/validate";
        }
        Map<String, String> params = parseQueryString(queryString.trim());
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!params.containsKey("vnp_Amount")) warnings.add("Thiếu vnp_Amount (số tiền x 100).");
        else if (!params.get("vnp_Amount").matches("\\d+")) errors.add("vnp_Amount phải là số nguyên.");
        if (!params.containsKey("vnp_TxnRef")) errors.add("Thiếu vnp_TxnRef.");
        if (!params.containsKey("vnp_OrderInfo")) warnings.add("Thiếu vnp_OrderInfo.");
        if (!params.containsKey("vnp_ReturnUrl")) errors.add("Thiếu vnp_ReturnUrl.");
        if (!params.containsKey("vnp_TmnCode")) errors.add("Thiếu vnp_TmnCode.");
        if (!params.containsKey("vnp_CreateDate")) warnings.add("Thiếu vnp_CreateDate (yyyyMMddHHmmss).");
        else if (params.get("vnp_CreateDate") != null && !params.get("vnp_CreateDate").matches("\\d{14}")) warnings.add("vnp_CreateDate nên có định dạng yyyyMMddHHmmss (14 chữ số).");
        if (!params.containsKey("vnp_SecureHash")) errors.add("Thiếu vnp_SecureHash (chữ ký HMAC-SHA512).");

        model.addAttribute("errors", errors);
        model.addAttribute("warnings", warnings);
        model.addAttribute("valid", errors.isEmpty());
        model.addAttribute("params", params);
        return "support/validate";
    }

    // ------------------- CHECK URL THANH TOÁN (full URL + secretKey → kiểm tra và trả URL đúng) -------------------
    @GetMapping("/check-payment-url")
    public String checkPaymentUrlForm() {
        return "support/check-payment-url";
    }

    @PostMapping("/check-payment-url")
    public String checkPaymentUrl(
            @RequestParam(required = false) String fullUrl,
            @RequestParam(required = false) String secretKey,
            Model model) {
        model.addAttribute("fullUrl", fullUrl);
        model.addAttribute("secretKey", secretKey);
        if (fullUrl == null || fullUrl.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập full URL thanh toán.");
            return "support/check-payment-url";
        }
        if (secretKey == null || secretKey.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập Secret Key.");
            return "support/check-payment-url";
        }
        fullUrl = fullUrl.trim();
        int q = fullUrl.indexOf('?');
        if (q < 0) {
            model.addAttribute("error", "URL không chứa query string (?).");
            return "support/check-payment-url";
        }
        String baseUrl = fullUrl.substring(0, q);
        String queryString = fullUrl.substring(q + 1);
        Map<String, String> params = parseQueryString(queryString);
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            model.addAttribute("error", "URL không chứa tham số vnp_SecureHash.");
            return "support/check-payment-url";
        }
        String hashData = buildHashData(params, true);
        String computedHash = VNPayUtils.hmacSHA512(secretKey.trim(), hashData);
        boolean valid = computedHash.equalsIgnoreCase(receivedHash);

        model.addAttribute("valid", valid);
        model.addAttribute("receivedHash", receivedHash);
        model.addAttribute("computedHash", computedHash);
        model.addAttribute("hashData", hashData);
        if (!valid) {
            String correctQuery = buildQueryStringWithHash(params, computedHash);
            model.addAttribute("correctUrl", baseUrl + "?" + correctQuery);
        }
        return "support/check-payment-url";
    }

    private String buildQueryStringWithHash(Map<String, String> params, String newHash) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        fieldNames.remove("vnp_SecureHash");
        fieldNames.remove("vnp_SecureHashType");
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        try {
            for (String key : fieldNames) {
                String val = params.get(key);
                if (val != null && !val.isEmpty()) {
                    if (sb.length() > 0) sb.append('&');
                    sb.append(URLEncoder.encode(key, StandardCharsets.US_ASCII.toString()))
                            .append('=').append(URLEncoder.encode(val, StandardCharsets.US_ASCII.toString()));
                }
            }
            if (sb.length() > 0) sb.append('&');
            sb.append("vnp_SecureHash=").append(newHash);
        } catch (UnsupportedEncodingException e) {
            log.warn("URL encode: {}", e.getMessage());
        }
        return sb.toString();
    }

    // ------------------- TẠO CHECKSUM (HMAC-SHA512, version 2.1.0) -------------------
    @GetMapping("/tao-checksum")
    public String taoChecksumForm() {
        return "support/tao-checksum";
    }

    @PostMapping("/tao-checksum")
    public String taoChecksum(
            @RequestParam(required = false) String queryString,
            @RequestParam(required = false) String secretKey,
            Model model) {
        model.addAttribute("queryString", queryString);
        model.addAttribute("secretKey", secretKey);
        if (queryString == null || queryString.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập chuỗi query (tham số, không bao gồm vnp_SecureHash).");
            return "support/tao-checksum";
        }
        if (secretKey == null || secretKey.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập Secret Key.");
            return "support/tao-checksum";
        }
        Map<String, String> params = parseQueryString(queryString.trim());
        String hashData = buildHashData(params, true);
        String vnpSecureHash = VNPayUtils.hmacSHA512(secretKey.trim(), hashData);
        model.addAttribute("vnpSecureHash", vnpSecureHash);
        model.addAttribute("hashData", hashData);
        model.addAttribute("version", "2.1.0");
        return "support/tao-checksum";
    }

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new TreeMap<>();
        if (query == null || query.isBlank()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1).trim();
            try {
                value = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException ignored) {
            }
            params.put(key, value);
        }
        return params;
    }

    private String buildHashData(Map<String, String> params) {
        return buildHashData(params, true);
    }

    private String buildHashData(Map<String, String> params, boolean excludeSecureHash) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        if (excludeSecureHash) {
            fieldNames.remove("vnp_SecureHash");
            fieldNames.remove("vnp_SecureHashType");
        }
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    String encoded = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                    if (hashData.length() > 0) hashData.append('&');
                    hashData.append(fieldName).append('=').append(encoded);
                } catch (UnsupportedEncodingException e) {
                    log.warn("URL encode error: {}", e.getMessage());
                }
            }
        }
        return hashData.toString();
    }
}
