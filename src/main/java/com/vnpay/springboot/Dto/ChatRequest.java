package com.vnpay.springboot.Dto;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {
    private String message;
    /** Lịch sử hội thoại gần nhất (user/assistant) để LLM có ngữ cảnh. */
    private List<ChatMessage> history = new ArrayList<>();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history != null ? history : new ArrayList<>();
    }
}
