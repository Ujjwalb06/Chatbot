package com.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_messages_session_id", columnList = "sessionId")
})
@Getter @NoArgsConstructor
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long sessionId;
    @Column(nullable = false) private String username;
    @Column(nullable = false) private String role;
    @Column(columnDefinition = "TEXT", nullable = false) private String content;
    @Column(nullable = false) private LocalDateTime timestamp = LocalDateTime.now();

    public ChatMessage(Long sessionId, String username, String role, String content) {
        this.sessionId = sessionId; this.username = username;
        this.role = role; this.content = content;
        this.timestamp = LocalDateTime.now();
    }
}