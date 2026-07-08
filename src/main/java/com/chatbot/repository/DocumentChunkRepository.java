package com.chatbot.repository;

import com.chatbot.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findBySessionIdOrderByChunkIndexAsc(Long sessionId);

    boolean existsBySessionId(Long sessionId);

    // ✅ Single bulk DELETE instead of N individual deletes
    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.sessionId = :sessionId")
    void deleteBySessionId(Long sessionId);
}