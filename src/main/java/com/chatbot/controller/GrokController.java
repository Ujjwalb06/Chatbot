package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ChatSessionRepository;
import com.chatbot.service.GrokService;
import com.chatbot.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GrokController {

    @Autowired
    private GrokService grokService;

    @Autowired
    private RagService ragService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body,
                                    Authentication authentication) throws Exception {
        String username = authentication.getName();

        // Validate inputs
        if (body.get("sessionId") == null) {
            throw new IllegalArgumentException("sessionId is required.");
        }

        Long sessionId = Long.valueOf(body.get("sessionId").toString());

        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty.");
        }

        Map<String, String> lastMsg = messages.get(messages.size() - 1);
        if (lastMsg.get("content") == null || lastMsg.get("content").isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        ChatSession session = chatSessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new RuntimeException("Invalid session or access denied."));

        // Save user's message
        chatMessageRepository.save(
            new ChatMessage(sessionId, username, lastMsg.get("role"), lastMsg.get("content"))
        );

        // Auto-rename session from first message
        if (session.getTitle().equals("New Chat")) {
            String firstContent = messages.get(0).get("content");
            String shortTitle = firstContent.length() > 40
                    ? firstContent.substring(0, 40) + "..."
                    : firstContent;
            session.setTitle(shortTitle);
            chatSessionRepository.save(session);
        }

        // Context window — last 10 messages only
        List<Map<String, String>> contextMessages = messages.size() > 10
                ? messages.subList(messages.size() - 10, messages.size())
                : messages;

        // ✅ RAG — retrieve relevant chunks for this question
        String userQuestion = lastMsg.get("content");
        String retrievedContext = ragService.retrieveRelevantContext(sessionId, userQuestion);

        // ✅ If relevant context found, inject it into messages as a system-level context message
        List<Map<String, String>> messagesWithContext = new ArrayList<>(contextMessages);
        if (retrievedContext != null) {
            // Inject context as the first message so LLM sees it before the conversation
            Map<String, String> contextMessage = Map.of(
                "role", "user",
                "content", "Use the following context from the uploaded document to answer "
                         + "the question. If the answer is not in the context, say so clearly "
                         + "and answer from your general knowledge.\n\n"
                         + "CONTEXT:\n" + retrievedContext
                         + "\n\nNow answer this question: " + userQuestion
            );
            // Replace last user message with the context-augmented version
            messagesWithContext = new ArrayList<>(contextMessages.subList(0, contextMessages.size() - 1));
            messagesWithContext.add(contextMessage);

            System.out.println("=== RAG: Context injected for session " + sessionId + " ===");
        }

        String answer = grokService.qa(messagesWithContext);

        // Save bot reply
        chatMessageRepository.save(new ChatMessage(sessionId, username, "assistant", answer));

        // ✅ Tell frontend whether RAG context was used
        boolean ragUsed = retrievedContext != null;
        return Map.of(
            "answer", answer,
            "sessionTitle", session.getTitle(),
            "ragUsed", ragUsed
        );
    }
}