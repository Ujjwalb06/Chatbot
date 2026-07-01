package com.chatbot.controller;

import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.service.EmailService;
import com.chatbot.util.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ Base URL from properties — swap localhost for real domain after deploy
    @Value("${app.base-url}")
    private String baseUrl;

    // Step 1: Send reset link to email
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();

        // ✅ Validate email format before anything else
        String email = body.get("email");
        Validator.validateEmail(email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        // Security: same message whether email exists or not (prevents email enumeration)
        if (userOpt.isEmpty()) {
            response.put("success", true);
            response.put("message", "If that email exists, a reset link has been sent.");
            return response;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + (15 * 60 * 1000); // 15 minutes

        user.setResetToken(token);
        user.setResetTokenExpiry(expiry);
        userRepository.save(user);

        // ✅ Uses baseUrl from properties — no hardcoded localhost
        String resetLink = baseUrl + "/reset-password.html?token=" + token;

        try {
            emailService.sendResetEmail(user.getEmail(), resetLink);
            System.out.println("=== PASSWORD RESET EMAIL SENT to: " + user.getEmail() + " ===");
        } catch (Exception e) {
            System.err.println("=== EMAIL SENDING FAILED: " + e.getMessage() + " ===");
            e.printStackTrace();
        }

        response.put("success", true);
        response.put("message", "If that email exists, a reset link has been sent.");
        return response;
    }

    // Step 2: Validate token and save new password
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();

        String token = body.get("token");
        String newPassword = body.get("newPassword");

        // ✅ Validate token and new password
        Validator.requireNonBlank(token, "Reset token");
        Validator.validatePassword(newPassword);

        Optional<User> userOpt = userRepository.findByResetToken(token);

        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid or expired reset link.");
            return response;
        }

        User user = userOpt.get();

        // ✅ Check token hasn't expired
        if (user.getResetTokenExpiry() == null
                || System.currentTimeMillis() > user.getResetTokenExpiry()) {
            response.put("success", false);
            response.put("message", "Reset link has expired. Please request a new one.");
            return response;
        }

        // Save hashed new password and clear the token so it can't be reused
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        System.out.println("=== PASSWORD RESET SUCCESSFUL for: " + user.getEmail() + " ===");

        response.put("success", true);
        response.put("message", "Password reset successfully! You can now log in.");
        return response;
    }
}