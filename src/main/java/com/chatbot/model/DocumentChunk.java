package com.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunks_session_id", columnList = "sessionId")
})
@Getter @Setter @NoArgsConstructor
public class DocumentChunk {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long sessionId;
    @Column(columnDefinition = "TEXT", nullable = false) private String content;
    @Column(nullable = false) private int chunkIndex;

    public DocumentChunk(Long sessionId, String content, int chunkIndex) {
        this.sessionId = sessionId; this.content = content; this.chunkIndex = chunkIndex;
    }
}