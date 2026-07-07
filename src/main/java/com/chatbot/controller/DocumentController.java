package com.chatbot.controller;

import com.chatbot.repository.ChatSessionRepository;
import com.chatbot.repository.DocumentChunkRepository;
import com.chatbot.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    @Autowired
    private RagService ragService;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") Long sessionId,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        String username = authentication.getName();

        chatSessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new RuntimeException("Session not found or access denied."));

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Please select a file to upload.");
            return response;
        }

        // ✅ Accept PDF and images
        String filename = file.getOriginalFilename();
        if (filename == null) {
            response.put("success", false);
            response.put("message", "Invalid file.");
            return response;
        }

        String filenameLower = filename.toLowerCase();
        boolean isPdf = filenameLower.endsWith(".pdf");
        boolean isImage = filenameLower.endsWith(".png")
                || filenameLower.endsWith(".jpg")
                || filenameLower.endsWith(".jpeg");

        if (!isPdf && !isImage) {
            response.put("success", false);
            response.put("message", "Only PDF, PNG, JPG files are supported.");
            return response;
        }

        // ✅ 10MB limit for PDFs, 5MB for images
        long maxSize = isPdf ? 10 * 1024 * 1024 : 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            response.put("success", false);
            response.put("message", isPdf ? "PDF must be under 10MB." : "Image must be under 5MB.");
            return response;
        }

        try {
            int chunkCount = ragService.processDocument(file, sessionId);
            String type = isPdf ? "PDF" : "image";
            response.put("success", true);
            response.put("message", type + " uploaded! " + chunkCount + " sections indexed.");
            response.put("chunkCount", chunkCount);
            response.put("fileType", isPdf ? "pdf" : "image");
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            System.err.println("=== UPLOAD ERROR: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Failed to process file: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> getDocumentStatus(
            @RequestParam("sessionId") Long sessionId,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        chatSessionRepository.findByIdAndUsername(sessionId, authentication.getName())
                .orElseThrow(() -> new RuntimeException("Session not found or access denied."));

        boolean hasDocument = documentChunkRepository.existsBySessionId(sessionId);
        int chunkCount = hasDocument
                ? documentChunkRepository.findBySessionIdOrderByChunkIndexAsc(sessionId).size()
                : 0;

        response.put("hasDocument", hasDocument);
        response.put("chunkCount", chunkCount);
        return response;
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clearDocument(
            @RequestParam("sessionId") Long sessionId,
            Authentication authentication) {

        chatSessionRepository.findByIdAndUsername(sessionId, authentication.getName())
                .orElseThrow(() -> new RuntimeException("Session not found or access denied."));

        documentChunkRepository.deleteBySessionId(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Document cleared. Back to normal chat mode.");
        return response;
    }
}
