package com.chatbot.service;

import com.chatbot.exception.ResourceNotFoundException;
import com.chatbot.exception.TokenLimitException;
import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final UserRepository userRepository;

    private static final int  TOKEN_LIMIT    = 50000;           // max tokens per window
    private static final long WINDOW_MS      = 60 * 60 * 1000; // 1 hour in milliseconds

    // =====================================================
    // Called BEFORE sending message to Groq
    // Checks if user has tokens available
    // =====================================================
    @Transactional
    public void checkAvailability(String username) {
        User user = findUser(username);

        // ✅ Step 1: Check if 1 hour has passed since last reset
        long now = System.currentTimeMillis();
        if (user.getTokenResetTime() == null || now >= user.getTokenResetTime()) {
            // Window expired — reset token count and start a new window
            user.setTokensUsed(0);
            user.setTokenResetTime(now + WINDOW_MS);
            userRepository.save(user);
            log.info("Token window reset for user: {}", username);
            return; // fresh window — definitely has tokens
        }

        // ✅ Step 2: Window still active — check if limit reached
        if (user.getTokensUsed() >= TOKEN_LIMIT) {
            long msRemaining      = user.getTokenResetTime() - now;
            long minutesRemaining = Math.max(1, msRemaining / 60000); // minimum 1 min shown
            log.warn("Token limit reached for user: {} ({} tokens used)", username, user.getTokensUsed());
            throw new TokenLimitException(minutesRemaining);
        }
    }

    // =====================================================
    // Called AFTER Groq responds — deduct actual tokens used
    // =====================================================
    @Transactional
    public void deductTokens(String username, int tokensUsed) {
        User user = findUser(username);
        int updated = user.getTokensUsed() + tokensUsed;
        user.setTokensUsed(updated);
        userRepository.save(user);
        log.info("Tokens deducted for {}: {} used, {} total this window",
                username, tokensUsed, updated);
    }

    // =====================================================
    // Called to show user their remaining tokens
    // =====================================================
    public int getRemainingTokens(String username) {
        User user = findUser(username);

        // If window expired, they effectively have full tokens
        if (user.getTokenResetTime() == null
                || System.currentTimeMillis() >= user.getTokenResetTime()) {
            return TOKEN_LIMIT;
        }

        return Math.max(0, TOKEN_LIMIT - user.getTokensUsed());
    }

    public long getMinutesUntilReset(String username) {
        User user = findUser(username);
        if (user.getTokenResetTime() == null) return 0;
        long msRemaining = user.getTokenResetTime() - System.currentTimeMillis();
        return Math.max(0, msRemaining / 60000);
    }

    // =====================================================
    // Private helper
    // =====================================================
    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}