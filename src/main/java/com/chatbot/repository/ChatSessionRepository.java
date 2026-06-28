package com.chatbot.repository;

import com.chatbot.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUsernameOrderByCreatedAtDesc(String username);
    Optional<ChatSession> findByIdAndUsername(Long id, String username);
}