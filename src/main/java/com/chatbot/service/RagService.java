package com.chatbot.service;

import com.chatbot.model.DocumentChunk;
import com.chatbot.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentChunkRepository chunkRepository;

    private static final int CHUNK_SIZE = 500; // words per chunk

    @Transactional
    public int processDocument(MultipartFile file, Long sessionId) throws IOException {
        String filename = file.getOriginalFilename().toLowerCase();
        String text;

        if (filename.endsWith(".pdf")) {
            text = extractFromPdf(file);
        } else if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            text = extractFromImage(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type.");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("No text could be extracted from this file.");
        }

        text = text.replaceAll("\\s+", " ").trim();

        // Clear old chunks for this session
        chunkRepository.deleteBySessionId(sessionId);

        // Build all chunk objects
        List<String> rawChunks = splitIntoChunks(text, CHUNK_SIZE);
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < rawChunks.size(); i++) {
            chunks.add(new DocumentChunk(sessionId, rawChunks.get(i), i));
        }

        // ✅ saveAll() = single batch INSERT instead of N individual saves
        chunkRepository.saveAll(chunks);
        log.info("RAG: {} chunks saved for session {}", chunks.size(), sessionId);
        return chunks.size();
    }

    public String retrieveRelevantContext(Long sessionId, String question) {
        List<DocumentChunk> allChunks = chunkRepository.findBySessionIdOrderByChunkIndexAsc(sessionId);
        if (allChunks.isEmpty()) return null;

        String[] keywords = question.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .split("\\s+");

        // Score each chunk by keyword frequency
        record ScoredChunk(String content, int score) {}

        List<ScoredChunk> scored = allChunks.stream().map(chunk -> {
            String lower = chunk.getContent().toLowerCase();
            int score = 0;
            for (String word : keywords) {
                if (word.length() > 3 && lower.contains(word)) score++;
            }
            return new ScoredChunk(chunk.getContent(), score);
        }).sorted((a, b) -> b.score() - a.score()).toList();

        StringBuilder context = new StringBuilder();
        int topN = Math.min(3, scored.size());
        for (int i = 0; i < topN; i++) {
            if (scored.get(i).score() > 0) context.append(scored.get(i).content()).append("\n\n");
        }

        // Fallback: return first chunk even if no keyword match (e.g. image metadata)
        if (context.isEmpty()) context.append(allChunks.get(0).getContent());

        return context.toString().trim();
    }

    // ===== PRIVATE HELPERS =====

    private String extractFromPdf(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractFromImage(MultipartFile file) throws IOException {
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) throw new IllegalArgumentException("Could not read image. Try a different file.");

        return "IMAGE_UPLOADED: " + file.getOriginalFilename()
             + " | Dimensions: " + img.getWidth() + "x" + img.getHeight() + "px"
             + " | Size: " + (file.getSize() / 1024) + "KB"
             + " | This session has an uploaded image. Describe it in detail when asked.";
    }

    private List<String> splitIntoChunks(String text, int size) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        int count = 0;
        for (String word : words) {
            chunk.append(word).append(" ");
            if (++count >= size) {
                chunks.add(chunk.toString().trim());
                chunk = new StringBuilder();
                count = 0;
            }
        }
        if (!chunk.isEmpty()) chunks.add(chunk.toString().trim());
        return chunks;
    }
}