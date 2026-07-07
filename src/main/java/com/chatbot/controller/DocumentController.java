package com.chatbot.controller;

import com.chatbot.dto.response.ApiResponse;
import com.chatbot.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") Long sessionId,
            Authentication auth) throws IOException {

        int chunkCount = documentService.upload(file, sessionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(
                "Document uploaded! " + chunkCount + " sections indexed.",
                Map.of("chunkCount", chunkCount)
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @RequestParam("sessionId") Long sessionId,
            Authentication auth) {

        boolean hasDocument = documentService.hasDocument(sessionId, auth.getName());
        int chunkCount = hasDocument ? documentService.getChunkCount(sessionId) : 0;
        return ResponseEntity.ok(ApiResponse.ok("Status fetched.",
                Map.of("hasDocument", hasDocument, "chunkCount", chunkCount)));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clear(
            @RequestParam("sessionId") Long sessionId,
            Authentication auth) {

        documentService.clear(sessionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Document cleared. Back to normal chat mode."));
    }
}