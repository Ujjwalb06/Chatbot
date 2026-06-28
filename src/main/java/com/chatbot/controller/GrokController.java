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
import java.util.Optional;

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
        Long sessionId = Long.valueOf(body.get("sessionId").toString());

        Optional<ChatSession> sessionOpt = chatSessionRepository.findByIdAndUsername(sessionId, username);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Invalid session");
        }
        ChatSession session = sessionOpt.get();

        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");

        // Save user's latest message
        if (!messages.isEmpty()) {
            Map<String, String> lastUserMsg = messages.get(messages.size() - 1);
            chatMessageRepository.save(
                new ChatMessage(sessionId, username, lastUserMsg.get("role"), lastUserMsg.get("content"))
            );
        }

        // ✅ Auto-rename session based on first message (if still "New Chat")
        if (session.getTitle().equals("New Chat") && !messages.isEmpty()) {
            String firstMsg = messages.get(0).get("content");
            String shortTitle = firstMsg.length() > 40 ? firstMsg.substring(0, 40) + "..." : firstMsg;
            session.setTitle(shortTitle);
            chatSessionRepository.save(session);
        }

        String answer = grokService.qa(messages);

        // Save bot's reply
        chatMessageRepository.save(
            new ChatMessage(sessionId, username, "assistant", answer)
        );

        return Map.of("answer", answer, "sessionTitle", session.getTitle());
    }
}