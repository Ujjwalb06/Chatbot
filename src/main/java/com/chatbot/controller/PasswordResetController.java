package com.chatbot.controller;

import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
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

    // ✅ Step 1: Request reset link
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String email = body.get("email");

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Don't reveal if email exists or not (security best practice)
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

        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        emailService.sendResetEmail(user.getEmail(), resetLink);

        response.put("success", true);
        response.put("message", "If that email exists, a reset link has been sent.");
        return response;
    }

    // ✅ Step 2: Reset password using token
   @PostMapping("/forgot-password")
public Map<String, Object> forgotPassword(@RequestBody Map<String, String> body) {
    Map<String, Object> response = new HashMap<>();
    String email = body.get("email");

    Optional<User> userOpt = userRepository.findByEmail(email);

    if (userOpt.isEmpty()) {
        response.put("success", true);
        response.put("message", "If that email exists, a reset link has been sent.");
        return response;
    }

    User user = userOpt.get();
    String token = UUID.randomUUID().toString();
    long expiry = System.currentTimeMillis() + (15 * 60 * 1000);

    user.setResetToken(token);
    user.setResetTokenExpiry(expiry);
    userRepository.save(user);

    String resetLink = "http://localhost:8080/reset-password.html?token=" + token;

    // ✅ ADD TRY-CATCH WITH LOGGING
    try {
        emailService.sendResetEmail(user.getEmail(), resetLink);
        System.out.println("=== EMAIL SENT SUCCESSFULLY to " + user.getEmail() + " ===");
    } catch (Exception e) {
        System.out.println("=== EMAIL SENDING FAILED ===");
        System.out.println("Error: " + e.getMessage());
        e.printStackTrace();
    }

    response.put("success", true);
    response.put("message", "If that email exists, a reset link has been sent.");
    return response;
}
}