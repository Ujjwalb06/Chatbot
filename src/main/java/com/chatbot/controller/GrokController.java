package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ChatSessionRepository;
import com.chatbot.service.GrokService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GrokController {

    @Autowired
    private GrokService grokService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body,
                                    Authentication authentication) throws Exception {
        String username = authentication.getName();

        // ✅ Validate sessionId is present
        if (body.get("sessionId") == null) {
            throw new IllegalArgumentException("sessionId is required.");
        }

        Long sessionId = Long.valueOf(body.get("sessionId").toString());

        // ✅ Validate messages list not null/empty
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty.");
        }

        // ✅ Validate last message has content
        Map<String, String> lastMsg = messages.get(messages.size() - 1);
        if (lastMsg.get("content") == null || lastMsg.get("content").isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        ChatSession session = chatSessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new RuntimeException("Invalid session or access denied."));

        // Save user's latest message
        chatMessageRepository.save(
            new ChatMessage(sessionId, username, lastMsg.get("role"), lastMsg.get("content"))
        );

        // Auto-rename session based on first user message
        if (session.getTitle().equals("New Chat")) {
            String firstContent = messages.get(0).get("content");
            String shortTitle = firstContent.length() > 40
                    ? firstContent.substring(0, 40) + "..."
                    : firstContent;
            session.setTitle(shortTitle);
            chatSessionRepository.save(session);
        }

        // ✅ Context window limit — only send last 10 messages to avoid token overflow
        List<Map<String, String>> contextMessages = messages.size() > 10
                ? messages.subList(messages.size() - 10, messages.size())
                : messages;

        String answer = grokService.qa(contextMessages);

        // Save bot reply
        chatMessageRepository.save(new ChatMessage(sessionId, username, "assistant", answer));

        return Map.of("answer", answer, "sessionTitle", session.getTitle());
    }
}