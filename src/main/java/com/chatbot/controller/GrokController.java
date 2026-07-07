package com.chatbot.controller;

import com.chatbot.dto.response.ChatResponse;
import com.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GrokController {

    // ✅ SOLID: Controller delegates to ChatService — no direct repo/service calls
    private final ChatService chatService;

    @PostMapping("/chat")
    public Map<String, Object> chat(
            @RequestBody Map<String, Object> body,
            Authentication auth) throws Exception {

        // Validate sessionId present
        if (body.get("sessionId") == null) {
            throw new IllegalArgumentException("sessionId is required.");
        }

        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");

        // Delegate all logic to ChatService
        ChatResponse result = chatService.chat(sessionId, messages, auth.getName());

        // ✅ Same response format frontend already expects — no breaking change
        return Map.of(
            "answer",       result.getAnswer(),
            "sessionTitle", result.getSessionTitle(),
            "ragUsed",      result.isRagUsed()
        );
    }
}