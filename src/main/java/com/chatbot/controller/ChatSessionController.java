package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
public class ChatSessionController {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // ✅ Get all sessions for logged-in user (sidebar list)
    @GetMapping
    public List<ChatSession> getSessions(Authentication authentication) {
        String username = authentication.getName();
        return chatSessionRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    // ✅ Create a new chat session
    @PostMapping
    public ChatSession createSession(Authentication authentication) {
        String username = authentication.getName();
        ChatSession session = new ChatSession(username, "New Chat");
        return chatSessionRepository.save(session);
    }

    // ✅ Get messages for a specific session
    @GetMapping("/{id}/messages")
    public List<ChatMessage> getMessages(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        Optional<ChatSession> session = chatSessionRepository.findByIdAndUsername(id, username);

        if (session.isEmpty()) return List.of();

        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(id);
    }

    // ✅ Rename a session
    @PutMapping("/{id}")
    public Map<String, Object> renameSession(@PathVariable Long id,
                                              @RequestBody Map<String, String> body,
                                              Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> response = new HashMap<>();

        Optional<ChatSession> sessionOpt = chatSessionRepository.findByIdAndUsername(id, username);

        if (sessionOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Session not found");
            return response;
        }

        ChatSession session = sessionOpt.get();
        session.setTitle(body.get("title"));
        chatSessionRepository.save(session);

        response.put("success", true);
        response.put("message", "Renamed successfully!");
        return response;
    }

    // ✅ Delete a session and its messages
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteSession(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> response = new HashMap<>();

        Optional<ChatSession> sessionOpt = chatSessionRepository.findByIdAndUsername(id, username);

        if (sessionOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Session not found");
            return response;
        }

        chatMessageRepository.deleteBySessionId(id);
        chatSessionRepository.deleteById(id);

        response.put("success", true);
        response.put("message", "Session deleted successfully!");
        return response;
    }
}