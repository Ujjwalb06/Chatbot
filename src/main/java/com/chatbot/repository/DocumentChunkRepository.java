package com.chatbot.repository;

import com.chatbot.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    // Get all chunks for a session in order
    List<DocumentChunk> findBySessionIdOrderByChunkIndexAsc(Long sessionId);

    // Delete all chunks when a session is deleted
    void deleteBySessionId(Long sessionId);

    // Check if a session already has chunks uploaded
    boolean existsBySessionId(Long sessionId);
}