package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import com.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    // ✅ SOLID: Controller delegates to ChatService — no direct repo calls
    private final ChatService chatService;

    @GetMapping
    public List<ChatSession> getSessions(Authentication auth) {
        return chatService.getSessions(auth.getName());
    }

    @PostMapping
    public ChatSession createSession(Authentication auth) {
        return chatService.createSession(auth.getName());
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessage> getMessages(@PathVariable Long id, Authentication auth) {
        return chatService.getMessages(id, auth.getName());
    }

    @PutMapping("/{id}")
    public Map<String, Object> renameSession(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        chatService.renameSession(id, auth.getName(), body.get("title"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Renamed successfully!");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteSession(@PathVariable Long id, Authentication auth) {
        chatService.deleteSession(id, auth.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Session deleted successfully!");
        return response;
    }
}