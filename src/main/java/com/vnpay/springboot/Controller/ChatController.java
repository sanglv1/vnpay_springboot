package com.vnpay.springboot.Controller;

import com.vnpay.springboot.Dto.ChatRequest;
import com.vnpay.springboot.Dto.ChatResponse;
import com.vnpay.springboot.Service.ChatbotService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatbotService chatbotService;

    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = chatbotService.getReply(request != null ? request : new ChatRequest());
        return new ChatResponse(reply);
    }
}
