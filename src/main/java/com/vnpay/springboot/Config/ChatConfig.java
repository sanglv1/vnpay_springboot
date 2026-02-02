package com.vnpay.springboot.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Cấu hình chatbot: provider (Gemini/OpenAI), API key, đường dẫn file tài liệu VNPAY.
 */
@Configuration
public class ChatConfig {

    @Value("${chat.provider:gemini}")
    private String provider;

    @Value("${chat.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${chat.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${chat.openai.api-key:}")
    private String openaiApiKey;

    @Value("${chat.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${chat.knowledge-path:}")
    private String knowledgePath;

    private String knowledgeContent = "";

    private final ResourceLoader resourceLoader;

    public ChatConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        String path = knowledgePath != null && !knowledgePath.isBlank()
                ? knowledgePath
                : "classpath:vnpay-knowledge.md";
        try {
            Resource resource = resourceLoader.getResource(path);
            if (resource.exists() && resource.isReadable()) {
                try (var is = resource.getInputStream()) {
                    knowledgeContent = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                }
            }
        } catch (IOException e) {
            // Bỏ qua nếu file không tồn tại hoặc không đọc được
        }
    }

    public String getProvider() {
        return provider == null ? "gemini" : provider.toLowerCase();
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public String getKnowledgeContent() {
        return knowledgeContent;
    }

    public boolean isLlmEnabled() {
        boolean useGemini = "gemini".equals(getProvider()) && geminiApiKey != null && !geminiApiKey.isBlank();
        boolean useOpenai = "openai".equals(getProvider()) && openaiApiKey != null && !openaiApiKey.isBlank();
        return useGemini || useOpenai;
    }
}
