package com.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;   // ✅ links message to a chat session

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public ChatMessage() {}

    public ChatMessage(Long sessionId, String username, String role, String content) {
        this.sessionId = sessionId;
        this.username = username;
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
}