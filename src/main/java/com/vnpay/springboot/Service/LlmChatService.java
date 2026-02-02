package com.vnpay.springboot.Service;

import com.vnpay.springboot.Config.ChatConfig;
import com.vnpay.springboot.Dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gọi API Gemini hoặc OpenAI để trả lời chat, dùng system prompt kèm tài liệu VNPAY.
 */
@Service
public class LlmChatService {

    private static final Logger log = LoggerFactory.getLogger(LlmChatService.class);

    private static final String GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String OPENAI_BASE = "https://api.openai.com/v1";

    private final ChatConfig chatConfig;
    private final WebClient.Builder webClientBuilder;

    public LlmChatService(ChatConfig chatConfig, WebClient.Builder webClientBuilder) {
        this.chatConfig = chatConfig;
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Trả lời dựa trên system prompt (có thể chứa tài liệu VNPAY) + lịch sử + tin nhắn mới.
     */
    public String reply(String systemPrompt, List<ChatMessage> history, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Vui lòng nhập câu hỏi.";
        }
        if ("gemini".equals(chatConfig.getProvider()) && isNotBlank(chatConfig.getGeminiApiKey())) {
            return callGemini(systemPrompt, history, userMessage);
        }
        if ("openai".equals(chatConfig.getProvider()) && isNotBlank(chatConfig.getOpenaiApiKey())) {
            return callOpenAI(systemPrompt, history, userMessage);
        }
        return "";
    }

    private String callGemini(String systemPrompt, List<ChatMessage> history, String userMessage) {
        String apiKey = chatConfig.getGeminiApiKey();
        String model = chatConfig.getGeminiModel();
        String url = GEMINI_BASE + "/" + model + ":generateContent?key=" + apiKey;

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage m : history) {
            String role = "user".equalsIgnoreCase(m.getRole()) ? "user" : "model";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", nullToEmpty(m.getContent())))
            ));
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", contents
        );

        try {
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return extractGeminiText(response);
        } catch (WebClientResponseException e) {
            log.warn("Gemini API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Không thể kết nối Gemini. Kiểm tra API key hoặc thử lại. (" + e.getStatusCode() + ")";
        } catch (Exception e) {
            log.warn("Gemini error", e);
            return "Đã xảy ra lỗi khi gọi Gemini. Vui lòng thử lại.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<String, Object> response) {
        if (response == null) return "Không có phản hồi.";
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            Object blockReason = response.get("promptFeedback");
            return "Gemini không trả lời. Thử hỏi lại hoặc kiểm tra nội dung.";
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return "Không có nội dung.";
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "Không có nội dung.";
        Object text = parts.get(0).get("text");
        return text != null ? text.toString().trim() : "Không có nội dung.";
    }

    private String callOpenAI(String systemPrompt, List<ChatMessage> history, String userMessage) {
        String apiKey = chatConfig.getOpenaiApiKey();
        String model = chatConfig.getOpenaiModel();
        String url = OPENAI_BASE + "/chat/completions";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage m : history) {
            String role = "user".equalsIgnoreCase(m.getRole()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", nullToEmpty(m.getContent())));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", 1024
        );

        try {
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return extractOpenAIText(response);
        } catch (WebClientResponseException e) {
            log.warn("OpenAI API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Không thể kết nối OpenAI. Kiểm tra API key hoặc thử lại. (" + e.getStatusCode() + ")";
        } catch (Exception e) {
            log.warn("OpenAI error", e);
            return "Đã xảy ra lỗi khi gọi OpenAI. Vui lòng thử lại.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractOpenAIText(Map<String, Object> response) {
        if (response == null) return "Không có phản hồi.";
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "Không có phản hồi.";
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) return "Không có nội dung.";
        Object content = message.get("content");
        return content != null ? content.toString().trim() : "Không có nội dung.";
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
