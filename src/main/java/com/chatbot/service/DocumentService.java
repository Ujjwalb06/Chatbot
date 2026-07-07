package com.chatbot.service;

import com.chatbot.exception.AccessDeniedException;
import com.chatbot.repository.ChatSessionRepository;
import com.chatbot.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final RagService ragService;
    private final ChatSessionRepository sessionRepository;
    private final DocumentChunkRepository chunkRepository;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".png", ".jpg", ".jpeg");
    private static final long MAX_PDF_SIZE   = 10 * 1024 * 1024; // 10 MB
    private static final long MAX_IMAGE_SIZE =  5 * 1024 * 1024; //  5 MB

    @Transactional
    public int upload(MultipartFile file, Long sessionId, String username) throws IOException {
        verifyOwnership(sessionId, username);
        validateFile(file);

        int chunkCount = ragService.processDocument(file, sessionId);
        log.info("Document uploaded for session {}: {} chunks indexed", sessionId, chunkCount);
        return chunkCount;
    }

    public boolean hasDocument(Long sessionId, String username) {
        verifyOwnership(sessionId, username);
        return chunkRepository.existsBySessionId(sessionId);
    }

    public int getChunkCount(Long sessionId) {
        return chunkRepository.findBySessionIdOrderByChunkIndexAsc(sessionId).size();
    }

    @Transactional
    public void clear(Long sessionId, String username) {
        verifyOwnership(sessionId, username);
        chunkRepository.deleteBySessionId(sessionId);
        log.info("Document cleared for session {} by {}", sessionId, username);
    }

    // ===== PRIVATE HELPERS =====

    private void verifyOwnership(Long sessionId, String username) {
        sessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new AccessDeniedException("Session not found or access denied."));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Please select a file to upload.");

        String filename = file.getOriginalFilename();
        if (filename == null) throw new IllegalArgumentException("Invalid file.");

        String lower = filename.toLowerCase();
        boolean valid = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!valid) throw new IllegalArgumentException("Only PDF, PNG, JPG files are supported.");

        boolean isPdf = lower.endsWith(".pdf");
        long maxSize  = isPdf ? MAX_PDF_SIZE : MAX_IMAGE_SIZE;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(isPdf ? "PDF must be under 10MB." : "Image must be under 5MB.");
        }
    }
}