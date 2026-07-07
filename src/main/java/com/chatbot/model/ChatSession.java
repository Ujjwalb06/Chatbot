package com.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions", indexes = {
    @Index(name = "idx_sessions_username", columnList = "username")
})
@Getter @Setter @NoArgsConstructor
public class ChatSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String title = "New Chat";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatSession(String username, String title) {
        this.username = username; this.title = title;
        this.createdAt = LocalDateTime.now();
    }
}