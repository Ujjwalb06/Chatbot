package com.chatbot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which session this chunk belongs to
    @Column(nullable = false)
    private Long sessionId;

    // The actual text content of this chunk
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Which chunk number this is (1, 2, 3...) for ordering
    @Column(nullable = false)
    private int chunkIndex;

    public DocumentChunk() {}

    public DocumentChunk(Long sessionId, String content, int chunkIndex) {
        this.sessionId = sessionId;
        this.content = content;
        this.chunkIndex = chunkIndex;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
}