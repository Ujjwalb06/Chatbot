package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ChatSessionRepository;
import com.chatbot.repository.DocumentChunkRepository;
import com.chatbot.util.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class ChatSessionController {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @GetMapping
    public List<ChatSession> getSessions(Authentication authentication) {
        return chatSessionRepository.findByUsernameOrderByCreatedAtDesc(authentication.getName());
    }

    @PostMapping
    public ChatSession createSession(Authentication authentication) {
        return chatSessionRepository.save(new ChatSession(authentication.getName(), "New Chat"));
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessage> getMessages(@PathVariable Long id, Authentication authentication) {
        chatSessionRepository.findByIdAndUsername(id, authentication.getName())
                .orElseThrow(() -> new RuntimeException("Session not found or access denied."));
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> renameSession(@PathVariable Long id,
                                              @RequestBody Map<String, String> body,
                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        String title = body.get("title");
        Validator.requireNonBlank(title, "Session title");

        ChatSession session = chatSessionRepository.findByIdAndUsername(id, authentication.getName())
                .orElseThrow(() -> new RuntimeException("Session not found or access denied."));

        session.setTitle(title.trim());
        chatSessionRepository.save(session);

        response.put("success", true);
        response.put("message", "Renamed successfully!");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteSession(@PathVariable Long id, Authentication authentication) {
        chatSessionRepository.findByIdAndUsername(id, authentication.getName())
                .orElseThrow(() -> new RuntimeException("Session not found or access denied."));

        // ✅ Delete messages, document chunks, then the session itself
        chatMessageRepository.deleteBySessionId(id);
        documentChunkRepository.deleteBySessionId(id);   // ← new line
        chatSessionRepository.deleteById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Session deleted successfully!");
        return response;
    }
}