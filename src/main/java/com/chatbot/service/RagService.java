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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    // ✅ Smaller chunks = more precise retrieval
    private static final int CHUNK_SIZE    = 300;
    // ✅ Overlap prevents losing context at chunk boundaries
    private static final int CHUNK_OVERLAP = 50;
    // Top N chunks to return as context
    private static final int TOP_N         = 3;

    // ✅ Stopwords — common words that add noise to TF-IDF scoring
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "the","is","are","was","were","be","been","being","have","has","had",
        "do","does","did","will","would","could","should","may","might","shall",
        "can","need","dare","used","ought","a","an","and","but","or","nor",
        "for","yet","so","at","by","in","of","on","to","up","as","it",
        "its","this","that","these","those","with","from","into","than","then",
        "when","where","who","which","what","how","if","not","no","each",
        "all","any","both","few","more","most","other","some","such","only",
        "own","same","too","very","just","also","about","after","before","over",
        "under","again","further","once","here","there","why","while"
    ));

    // =====================================================
    // PUBLIC: PROCESS UPLOADED FILE
    // =====================================================
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

        extractedText = extractedText.replaceAll("\\s+", " ").trim();

        // Delete old chunks
        documentChunkRepository.deleteBySessionId(sessionId);

        // ✅ Split with overlap and batch save
        List<String> chunks = splitWithOverlap(extractedText, CHUNK_SIZE, CHUNK_OVERLAP);
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            chunkEntities.add(new DocumentChunk(sessionId, chunks.get(i), i));
        }
        documentChunkRepository.saveAll(chunkEntities); // single batch INSERT

        System.out.println("=== RAG: Saved " + chunks.size() + " chunks for session " + sessionId + " ===");
        return chunks.size();
    }

    // =====================================================
    // PUBLIC: RETRIEVE RELEVANT CONTEXT USING TF-IDF
    // =====================================================
    public String retrieveRelevantContext(Long sessionId, String question) {
        List<DocumentChunk> allChunks = documentChunkRepository
                .findBySessionIdOrderByChunkIndexAsc(sessionId);

        if (allChunks.isEmpty()) return null;

        // Image session fallback — return first chunk directly
        if (allChunks.get(0).getContent().startsWith("IMAGE_UPLOADED:")) {
            return allChunks.get(0).getContent();
        }

        // ✅ Tokenize and clean the question
        List<String> queryTerms = tokenize(question);
        if (queryTerms.isEmpty()) return null;

        // ✅ Compute IDF for each query term across all chunks
        Map<String, Double> idfMap = computeIdf(queryTerms, allChunks);

        // ✅ Score each chunk using TF-IDF
        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : allChunks) {
            double score = computeTfIdfScore(chunk.getContent(), queryTerms, idfMap);
            if (score > 0) scored.add(new ScoredChunk(chunk.getContent(), score));
        }

        if (scored.isEmpty()) {
            // Fallback: return first chunk if no TF-IDF match at all
            return allChunks.get(0).getContent();
        }

        // Sort by score descending, take top N
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        StringBuilder context = new StringBuilder();
        int limit = Math.min(TOP_N, scored.size());
        for (int i = 0; i < limit; i++) {
            context.append(scored.get(i).content).append("\n\n");
        }

        System.out.println("=== RAG: TF-IDF selected " + limit + " chunks for session " + sessionId + " ===");
        return context.toString().trim();
    }

    // =====================================================
    // PRIVATE: TF-IDF COMPUTATION
    // =====================================================

    /**
     * Compute IDF (Inverse Document Frequency) for each query term.
     * IDF = log( totalChunks / chunksContainingTerm )
     * Rare words get high IDF; common words get low IDF.
     */
    private Map<String, Double> computeIdf(List<String> queryTerms, List<DocumentChunk> chunks) {
        Map<String, Double> idf = new HashMap<>();
        int totalChunks = chunks.size();

        for (String term : queryTerms) {
            long chunksWithTerm = chunks.stream()
                    .filter(c -> c.getContent().toLowerCase().contains(term))
                    .count();

            if (chunksWithTerm > 0) {
                // +1 smoothing to avoid division by zero
                idf.put(term, Math.log((double) totalChunks / (chunksWithTerm + 1)) + 1);
            } else {
                idf.put(term, 0.0);
            }
        }
        return idf;
    }

    /**
     * Compute TF-IDF score for a single chunk against the query terms.
     * TF = (occurrences of term in chunk) / (total words in chunk)
     * Score = sum of TF * IDF for all query terms
     */
    private double computeTfIdfScore(String chunkContent, List<String> queryTerms, Map<String, Double> idfMap) {
        List<String> chunkTokens = tokenize(chunkContent);
        if (chunkTokens.isEmpty()) return 0.0;

        // Count frequency of each token in this chunk
        Map<String, Long> termFreq = chunkTokens.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        double score = 0.0;
        for (String term : queryTerms) {
            long freq = termFreq.getOrDefault(term, 0L);
            if (freq > 0) {
                // TF = frequency / total tokens in chunk
                double tf = (double) freq / chunkTokens.size();
                double idf = idfMap.getOrDefault(term, 0.0);
                score += tf * idf;
            }
        }
        return score;
    }

    // =====================================================
    // PRIVATE: TOKENIZATION
    // =====================================================

    /**
     * Tokenize text: lowercase, remove punctuation, remove stopwords,
     * keep only words with length >= 3.
     */
    private List<String> tokenize(String text) {
        return Arrays.stream(
                text.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", " ")
                    .split("\\s+")
                )
                .filter(w -> w.length() >= 3)
                .filter(w -> !STOPWORDS.contains(w))
                .collect(Collectors.toList());
    }

    // =====================================================
    // PRIVATE: CHUNKING WITH OVERLAP
    // =====================================================

    /**
     * Split text into overlapping chunks.
     * Overlap prevents losing context at chunk boundaries.
     * Example: chunk1 = words 0-299, chunk2 = words 250-549, etc.
     */
    private List<String> splitWithOverlap(String text, int chunkSize, int overlap) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;

        for (int i = 0; i < words.length; i += step) {
            int end = Math.min(i + chunkSize, words.length);
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < end; j++) {
                chunk.append(words[j]).append(" ");
            }
            chunks.add(chunk.toString().trim());
            if (end == words.length) break;
        }
        return chunks;
    }

    // =====================================================
    // PRIVATE: FILE EXTRACTION
    // =====================================================

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromImage(MultipartFile file) throws IOException {
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) {
            throw new IllegalArgumentException("Could not read image file. Please try a different image.");
        }
        return "IMAGE_UPLOADED: " + file.getOriginalFilename()
             + " | Dimensions: " + img.getWidth() + "x" + img.getHeight() + "px"
             + " | Size: " + (file.getSize() / 1024) + "KB"
             + " | This session has an uploaded image. Describe it in detail when asked.";
    }

    // =====================================================
    // PRIVATE: HELPER CLASS
    // =====================================================

    private static class ScoredChunk {
        String content;
        double score;
        ScoredChunk(String content, double score) {
            this.content = content;
            this.score   = score;
        }
    }
}