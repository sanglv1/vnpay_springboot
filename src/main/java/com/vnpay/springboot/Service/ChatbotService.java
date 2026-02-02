package com.vnpay.springboot.Service;

import com.vnpay.springboot.Config.ChatConfig;
import com.vnpay.springboot.Dto.ChatMessage;
import com.vnpay.springboot.Dto.ChatRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Chatbot: nếu cấu hình API Gemini/OpenAI thì dùng LLM kèm tài liệu VNPAY;
 * không thì fallback sang rule-based.
 */
@Service
public class ChatbotService {

    private static final String BASE_SYSTEM_PROMPT =
            "Bạn là trợ lý kỹ thuật chuyên sâu về Cổng thanh toán VNPAY (CTT VNPAY). "
            + "Nhiệm vụ: trả lời chính xác, rõ ràng và hữu ích về tích hợp VNPAY (checksum, return URL, IPN, truy vấn giao dịch, hoàn tiền, tham số, sandbox, API...). "
            + "Quy tắc: (1) Ưu tiên thông tin trong tài liệu đính kèm; nếu có thì trích dẫn hoặc dựa vào đó để trả lời. "
            + "(2) Khi cần giải thích kỹ thuật, hãy giải thích từng bước hoặc đưa ví dụ (code, curl, tham số) nếu phù hợp. "
            + "(3) Trả lời bằng tiếng Việt, dùng markdown (**, bullet, code block) cho dễ đọc. "
            + "(4) Nếu câu hỏi mơ hồ, hỏi lại cho rõ; nếu ngoài phạm vi VNPAY, trả lời lịch sự và gợi ý hỏi về VNPAY.";

    private final ChatConfig chatConfig;
    private final LlmChatService llmChatService;

    public ChatbotService(ChatConfig chatConfig, LlmChatService llmChatService) {
        this.chatConfig = chatConfig;
        this.llmChatService = llmChatService;
    }

    public String getReply(ChatRequest request) {
        String message = request != null ? request.getMessage() : null;
        List<ChatMessage> history = request != null && request.getHistory() != null
                ? request.getHistory()
                : List.of();

        if (message == null || message.isBlank()) {
            return "Bạn có thể hỏi về: checksum/chữ ký, return URL, hoàn tiền, truy vấn giao dịch, encode URL, demo thanh toán, hoặc gõ \"menu\" để xem gợi ý.";
        }

        if (chatConfig.isLlmEnabled()) {
            String systemPrompt = buildSystemPrompt();
            String reply = llmChatService.reply(systemPrompt, history, message);
            if (reply != null && !reply.isBlank()) {
                return reply;
            }
        }

        return getRuleBasedReply(message);
    }

    private String buildSystemPrompt() {
        String knowledge = chatConfig.getKnowledgeContent();
        if (knowledge != null && !knowledge.isBlank()) {
            return BASE_SYSTEM_PROMPT + "\n\n---\nTài liệu tham khảo VNPAY (dùng để trả lời chính xác):\n\n" + knowledge;
        }
        return BASE_SYSTEM_PROMPT;
    }

    private String getRuleBasedReply(String userMessage) {
        String normalized = userMessage.trim().toLowerCase(Locale.ROOT);

        if (matches(normalized, "chào|hello|hi|xin chào|helo")) {
            return "Chào bạn! Tôi là trợ lý Công cụ hỗ trợ CTT VNPAY. Bạn có thể hỏi về tích hợp thanh toán VNPAY, checksum, return URL, hoàn tiền, truy vấn giao dịch... Gõ \"menu\" để xem gợi ý.";
        }
        if (matches(normalized, "menu|gợi ý|trợ giúp|help|hướng dẫn")) {
            return "📌 **Gợi ý câu hỏi:**\n"
                    + "• \"Checksum là gì?\" / \"Cách tạo chữ ký?\"\n"
                    + "• \"Return URL là gì?\"\n"
                    + "• \"Cách hoàn tiền?\" / \"Refund\"\n"
                    + "• \"Truy vấn giao dịch\" / \"Query\"\n"
                    + "• \"Encode URL\"\n"
                    + "• \"Demo thanh toán ở đâu?\"\n"
                    + "Trang **Công cụ hỗ trợ CTT VNPAY** có sẵn Encode, Check URL, Tạo checksum và Demo / Thử nghiệm để bạn dùng.";
        }
        if (matches(normalized, "checksum|chữ ký|chữ ky|securehash|hmac|sha512")) {
            return "**Checksum (chữ ký VNPAY):**\n"
                    + "• Dùng HMAC-SHA512 với Secret Key (version 2.1.0).\n"
                    + "• Chuỗi hash: sắp xếp tham số A-Z, bỏ vnp_SecureHash & vnp_SecureHashType, nối key=value bằng &, URL-encode value.\n"
                    + "• Trên trang **Công cụ hỗ trợ** có mục **Tạo checksum** và **Check URL thanh toán** để kiểm tra/tạo chữ ký.";
        }
        if (matches(normalized, "return url|returnurl|url return|callback|redirect")) {
            return "**Return URL** là địa chỉ VNPAY redirect người dùng sau khi thanh toán xong. Bạn cấu hình trong merchant (vnpay.return-url). Trên demo này return URL mặc định là: /vnpay_support/demo/vnpay-return. Cần đăng ký URL với VNPAY và dùng HTTPS ở môi trường thật.";
        }
        if (matches(normalized, "hoàn tiền|refund|hoan tien")) {
            return "**Hoàn tiền (Refund):** Gọi API Refund của VNPAY với TxnRef, TransactionNo, số tiền, TransDate, TransType, CreateBy. Trên trang **Demo thanh toán** có mục **Hoàn tiền giao dịch** để thử. Chi tiết API xem tài liệu merchant VNPAY.";
        }
        if (matches(normalized, "truy vấn|query|tra cứu|truy van|querydr")) {
            return "**Truy vấn giao dịch (Query):** Dùng API Query Transaction (QueryDR) với TxnRef, TransDate (yyyyMMddHHmmss), có thể thêm TransactionNo. Trên **Demo thanh toán** có mục **Truy vấn giao dịch** để thử.";
        }
        if (matches(normalized, "encode|decode|url encode|mã hóa")) {
            return "**Encode/Decode URL:** Dùng UTF-8. Trên **Công cụ hỗ trợ** có mục **Encode / Decode** để mã hóa hoặc giải mã chuỗi.";
        }
        if (matches(normalized, "demo|thử|test|tạo đơn")) {
            return "**Demo thanh toán:** Vào **Demo thanh toán** trên trang chủ (hoặc /demo) để: Tạo đơn hàng, Truy vấn giao dịch, Hoàn tiền. **Thống kê đơn hàng** nằm trong cùng mục Demo.";
        }
        if (matches(normalized, "công cụ|cong cu|support|check url")) {
            return "**Công cụ hỗ trợ** (menu **Công cụ hỗ trợ** hoặc /support): Encode/Decode, Check URL thanh toán (kiểm tra chữ ký + trả URL đúng), Tạo checksum (HMAC-SHA512 2.1.0).";
        }
        if (matches(normalized, "cảm ơn|cam on|thanks|tạm biệt|bye")) {
            return "Không có gì! Chúc bạn tích hợp VNPAY thuận lợi. Cần thêm hỗ trợ cứ hỏi.";
        }

        return "Tôi chưa hiểu rõ câu hỏi. Bạn thử hỏi về: **checksum**, **return URL**, **hoàn tiền**, **truy vấn**, **encode**, hoặc gõ **menu** để xem gợi ý. (Bạn có thể bật Gemini/OpenAI trong cấu hình để trợ lý trả lời theo tài liệu VNPAY.)";
    }

    private boolean matches(String text, String pattern) {
        for (String p : pattern.split("\\|")) {
            if (text.contains(p.trim())) return true;
        }
        return false;
    }
}
