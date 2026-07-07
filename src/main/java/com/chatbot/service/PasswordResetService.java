package com.chatbot.service;

import com.chatbot.exception.ResourceNotFoundException;
import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.util.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final long EXPIRY_MS = 15 * 60 * 1000; // 15 minutes

    @Transactional
    public void sendResetLink(String email) {
        Validator.validateEmail(email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        // Security: same response whether email exists or not (prevents enumeration)
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email: {}", email);
            return;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setResetTokenExpiry(System.currentTimeMillis() + EXPIRY_MS);
        userRepository.save(user);

        String resetLink = baseUrl + "/reset-password.html?token=" + token;

        try {
            emailService.sendResetEmail(user.getEmail(), resetLink);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Validator.requireNonBlank(token, "Reset token");
        Validator.validatePassword(newPassword);

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link."));

        if (user.getResetTokenExpiry() == null || System.currentTimeMillis() > user.getResetTokenExpiry()) {
            throw new IllegalArgumentException("Reset link has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset successful for: {}", user.getEmail());
    }
}