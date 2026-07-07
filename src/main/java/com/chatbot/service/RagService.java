package com.chatbot.service;

import com.chatbot.model.DocumentChunk;
import com.chatbot.repository.DocumentChunkRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    private static final int CHUNK_SIZE = 500;

    // ===== PROCESS FILE — PDF or Image =====
    public int processDocument(MultipartFile file, Long sessionId) throws IOException {
        String filename = file.getOriginalFilename().toLowerCase();
        String extractedText;

        if (filename.endsWith(".pdf")) {
            extractedText = extractTextFromPdf(file);
        } else if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            extractedText = extractTextFromImage(file);
        } else {
            throw new IllegalArgumentException("Only PDF, PNG, JPG files are supported.");
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException("Could not extract any content from this file.");
        }

        // Clean whitespace
        extractedText = extractedText.replaceAll("\\s+", " ").trim();

        // Delete old chunks for this session
        documentChunkRepository.deleteBySessionId(sessionId);

        // Split and save chunks
        List<String> chunks = splitIntoChunks(extractedText, CHUNK_SIZE);
        for (int i = 0; i < chunks.size(); i++) {
            documentChunkRepository.save(new DocumentChunk(sessionId, chunks.get(i), i));
        }

        System.out.println("=== RAG: Saved " + chunks.size() + " chunks for session " + sessionId + " ===");
        return chunks.size();
    }

    // ===== EXTRACT TEXT FROM PDF (PDFBox 3.x API) =====
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        // ✅ PDFBox 3.x uses Loader.loadPDF(byte[]) instead of PDDocument.load()
        byte[] bytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    // ===== EXTRACT TEXT FROM IMAGE =====
    // Stores image metadata as searchable text context
    // The actual image is sent to Groq vision in GrokController
    private String extractTextFromImage(MultipartFile file) throws IOException {
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) {
            throw new IllegalArgumentException("Could not read image file. Please try a different image.");
        }

        String filename = file.getOriginalFilename();
        long sizeKb = file.getSize() / 1024;

        // Store image metadata as a chunk so RAG knows an image is uploaded
        return "IMAGE_UPLOADED: " + filename + " | "
             + "Dimensions: " + img.getWidth() + "x" + img.getHeight() + "px | "
             + "Size: " + sizeKb + "KB | "
             + "This session has an uploaded image. "
             + "When asked about this image, describe what you see in detail.";
    }

    // ===== RETRIEVE RELEVANT CHUNKS FOR A QUESTION =====
    public String retrieveRelevantContext(Long sessionId, String question) {
        List<DocumentChunk> allChunks = documentChunkRepository
                .findBySessionIdOrderByChunkIndexAsc(sessionId);

        if (allChunks.isEmpty()) return null;

        String[] questionWords = question.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .split("\\s+");

        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (DocumentChunk chunk : allChunks) {
            String chunkLower = chunk.getContent().toLowerCase();
            int score = 0;
            for (String word : questionWords) {
                if (word.length() > 3 && chunkLower.contains(word)) score++;
            }
            scoredChunks.add(new ScoredChunk(chunk.getContent(), score));
        }

        scoredChunks.sort((a, b) -> b.score - a.score);

        StringBuilder context = new StringBuilder();
        int topN = Math.min(3, scoredChunks.size());
        for (int i = 0; i < topN; i++) {
            if (scoredChunks.get(i).score > 0) {
                context.append(scoredChunks.get(i).content).append("\n\n");
            }
        }

        // If no keyword match, return first chunk anyway (for image sessions)
        if (context.length() == 0 && !allChunks.isEmpty()) {
            context.append(allChunks.get(0).getContent());
        }

        return context.toString().trim();
    }

    // ===== Check if session has an image uploaded =====
    public boolean isImageSession(Long sessionId) {
        List<DocumentChunk> chunks = documentChunkRepository
                .findBySessionIdOrderByChunkIndexAsc(sessionId);
        return !chunks.isEmpty() && chunks.get(0).getContent().startsWith("IMAGE_UPLOADED:");
    }

    private List<String> splitIntoChunks(String text, int chunkSize) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        int wordCount = 0;

        for (String word : words) {
            chunk.append(word).append(" ");
            wordCount++;
            if (wordCount >= chunkSize) {
                chunks.add(chunk.toString().trim());
                chunk = new StringBuilder();
                wordCount = 0;
            }
        }
        if (chunk.length() > 0) chunks.add(chunk.toString().trim());
        return chunks;
    }

    private static class ScoredChunk {
        String content;
        int score;
        ScoredChunk(String content, int score) {
            this.content = content;
            this.score = score;
        }
    }
}
