package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class ChatHistoryController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    //  Get chat history for logged-in user
    @GetMapping
    public List<ChatMessage> getHistory(Authentication authentication) {
        String username = authentication.getName();
        return chatMessageRepository.findByUsernameOrderByTimestampAsc(username);
    }

    //  Clear chat history for logged-in user
   @DeleteMapping
public String clearHistory(Authentication authentication) {
    System.out.println("=== DELETE /api/history HIT ===");
    System.out.println("Authentication object: " + authentication);
    
    if (authentication == null) {
        System.out.println("Authentication is NULL!");
        return "Unauthorized";
    }
    
    String username = authentication.getName();
    System.out.println("Username: " + username);
    chatMessageRepository.deleteByUsername(username);
    return "Chat history cleared!";
}
}