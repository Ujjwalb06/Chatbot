package com.chatbot.service;

import com.chatbot.dto.response.ChatResponse;
import com.chatbot.exception.AccessDeniedException;
import com.chatbot.exception.ResourceNotFoundException;
import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatSession;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ChatSessionRepository;
import com.chatbot.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final DocumentChunkRepository chunkRepository;
    private final GrokService grokService;
    private final RagService ragService;

    private static final int CONTEXT_WINDOW = 10; // max messages sent to LLM
    private static final int MAX_TITLE_LENGTH = 40;

    // ===== SESSION MANAGEMENT =====

    public List<ChatSession> getSessions(String username) {
        return sessionRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public ChatSession createSession(String username) {
        ChatSession session = new ChatSession(username, "New Chat");
        return sessionRepository.save(session);
    }

    public List<ChatMessage> getMessages(Long sessionId, String username) {
        verifySessionOwnership(sessionId, username);
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    @Transactional
    public void renameSession(Long sessionId, String username, String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("Session title cannot be empty.");
        }
        ChatSession session = verifySessionOwnership(sessionId, username);
        session.setTitle(newTitle.trim());
        sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId, String username) {
        verifySessionOwnership(sessionId, username);
        // Delete all related data atomically — @Transactional ensures all-or-nothing
        messageRepository.deleteBySessionId(sessionId);
        chunkRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        log.info("Session {} deleted by {}", sessionId, username);
    }

    // ===== CHAT (RAG + LLM) =====

    @Transactional
    public ChatResponse chat(Long sessionId, List<Map<String, String>> messages, String username) throws Exception {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty.");
        }

        Map<String, String> lastMsg = messages.get(messages.size() - 1);
        if (lastMsg.get("content") == null || lastMsg.get("content").isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        ChatSession session = verifySessionOwnership(sessionId, username);

        // Persist user's message
        messageRepository.save(new ChatMessage(sessionId, username, lastMsg.get("role"), lastMsg.get("content")));

        // Auto-rename session from first message
        if ("New Chat".equals(session.getTitle())) {
            String firstContent = messages.get(0).get("content");
            String title = firstContent.length() > MAX_TITLE_LENGTH
                    ? firstContent.substring(0, MAX_TITLE_LENGTH) + "..."
                    : firstContent;
            session.setTitle(title);
            sessionRepository.save(session);
        }

        // Trim to context window
        List<Map<String, String>> contextMessages = messages.size() > CONTEXT_WINDOW
                ? messages.subList(messages.size() - CONTEXT_WINDOW, messages.size())
                : messages;

        // RAG — retrieve relevant chunks and augment the prompt
        String userQuestion = lastMsg.get("content");
        String retrievedContext = ragService.retrieveRelevantContext(sessionId, userQuestion);
        List<Map<String, String>> finalMessages = augmentWithContext(contextMessages, retrievedContext, userQuestion);

        if (retrievedContext != null) {
            log.info("RAG context injected for session {}", sessionId);
        }

        // Call LLM
        String answer = grokService.qa(finalMessages);

        // Persist assistant reply
        messageRepository.save(new ChatMessage(sessionId, username, "assistant", answer));

        return new ChatResponse(answer, session.getTitle(), retrievedContext != null);
    }

    // ===== PRIVATE HELPERS =====

    private ChatSession verifySessionOwnership(Long sessionId, String username) {
        return sessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new AccessDeniedException("Session not found or access denied."));
    }

    private List<Map<String, String>> augmentWithContext(
            List<Map<String, String>> messages, String context, String question) {

        if (context == null) return messages;

        // Replace last user message with a context-augmented version
        List<Map<String, String>> augmented = new ArrayList<>(messages.subList(0, messages.size() - 1));
        augmented.add(Map.of(
            "role", "user",
            "content", "Use the following context from the uploaded document to answer the question.\n"
                     + "If the answer is not in the context, say so and answer from general knowledge.\n\n"
                     + "CONTEXT:\n" + context + "\n\nQUESTION: " + question
        ));
        return augmented;
    }
}