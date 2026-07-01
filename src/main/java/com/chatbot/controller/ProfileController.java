package com.chatbot.controller;

import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.util.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public Map<String, Object> getProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found."));

        Map<String, Object> response = new HashMap<>();
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        return response;
    }

    @PutMapping
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body,
                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        String username = authentication.getName();

        // ✅ Validate name and email
        String newName = body.get("name");
        String newEmail = body.get("email");
        Validator.requireNonBlank(newName, "Name");
        Validator.validateEmail(newEmail);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found."));

        // Check email not taken by someone else
        if (!newEmail.equals(user.getEmail())) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                response.put("success", false);
                response.put("message", "Email already in use.");
                return response;
            }
        }

        user.setName(newName);
        user.setEmail(newEmail);
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Profile updated successfully!");
        return response;
    }

    @PutMapping("/password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body,
                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        String username = authentication.getName();

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        // ✅ Validate both fields
        Validator.requireNonBlank(currentPassword, "Current password");
        Validator.validatePassword(newPassword);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            response.put("success", false);
            response.put("message", "Current password is incorrect.");
            return response;
        }

        // ✅ Prevent setting the same password again
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            response.put("success", false);
            response.put("message", "New password must be different from current password.");
            return response;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Password changed successfully!");
        return response;
    }
}