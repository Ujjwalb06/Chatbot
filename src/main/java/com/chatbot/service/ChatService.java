package com.chatbot.service;

import com.chatbot.dto.response.ChatResponse;
import com.chatbot.exception.AccessDeniedException;
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

    // ✅ NEW — inject TokenService
    private final TokenService tokenService;

    private static final int CONTEXT_WINDOW  = 10;
    private static final int MAX_TITLE_LENGTH = 40;

    // ===== SESSION MANAGEMENT =====

    public List<ChatSession> getSessions(String username) {
        return sessionRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public ChatSession createSession(String username) {
        return sessionRepository.save(new ChatSession(username, "New Chat"));
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
        messageRepository.deleteBySessionId(sessionId);
        chunkRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        log.info("Session {} deleted by {}", sessionId, username);
    }

    // ===== CHAT (RAG + TOKEN CHECK + LLM) =====

    @Transactional
    public ChatResponse chat(Long sessionId, List<Map<String, String>> messages,
                             String username) throws Exception {

        // Validate inputs
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty.");
        }
        Map<String, String> lastMsg = messages.get(messages.size() - 1);
        if (lastMsg.get("content") == null || lastMsg.get("content").isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        ChatSession session = verifySessionOwnership(sessionId, username);

        // ✅ STEP 1: Check tokens BEFORE doing anything
        // Throws TokenLimitException if limit reached — stops here
        tokenService.checkAvailability(username);

        // Persist user's message
        messageRepository.save(new ChatMessage(
                sessionId, username, lastMsg.get("role"), lastMsg.get("content")));

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

        // RAG — retrieve relevant chunks and augment prompt
        String userQuestion   = lastMsg.get("content");
        String retrievedContext = ragService.retrieveRelevantContext(sessionId, userQuestion);
        List<Map<String, String>> finalMessages =
                augmentWithContext(contextMessages, retrievedContext, userQuestion);

        if (retrievedContext != null) {
            log.info("RAG context injected for session {}", sessionId);
        }

        // ✅ STEP 2: Call Groq — now returns GrokResponse (answer + tokensUsed)
        GrokService.GrokResponse groqResult = grokService.qa(finalMessages);

        // ✅ STEP 3: Deduct ACTUAL tokens used from Groq's response
        tokenService.deductTokens(username, groqResult.tokensUsed());
        log.info("Tokens deducted for {}: {} tokens this message, {} remaining",
                username,
                groqResult.tokensUsed(),
                tokenService.getRemainingTokens(username));

        // Persist assistant reply
        messageRepository.save(new ChatMessage(
                sessionId, username, "assistant", groqResult.answer()));

        // ✅ Include remaining tokens in response so frontend can display it
        return new ChatResponse(
                groqResult.answer(),
                session.getTitle(),
                retrievedContext != null,
                tokenService.getRemainingTokens(username)
        );
    }

    // ===== PRIVATE HELPERS =====

    private ChatSession verifySessionOwnership(Long sessionId, String username) {
        return sessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new AccessDeniedException("Session not found or access denied."));
    }

    private List<Map<String, String>> augmentWithContext(
            List<Map<String, String>> messages, String context, String question) {
        if (context == null) return messages;
        List<Map<String, String>> augmented =
                new ArrayList<>(messages.subList(0, messages.size() - 1));
        augmented.add(Map.of(
            "role", "user",
            "content", "Use the following context from the uploaded document to answer.\n"
                     + "If not in context, say so and answer from general knowledge.\n\n"
                     + "CONTEXT:\n" + context + "\n\nQUESTION: " + question
        ));
        return augmented;
    }
}