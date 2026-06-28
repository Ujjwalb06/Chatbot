package com.chatbot.controller;

import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
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

    // ✅ Get current logged-in user's profile
    @GetMapping
    public Map<String, Object> getProfile(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        Map<String, Object> response = new HashMap<>();

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
        }

        return response;
    }

    // ✅ Update name/email
    @PutMapping
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body,
                                              Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }

        User user = userOpt.get();

        String newEmail = body.get("email");
        // check if email is being changed to one already used by someone else
        if (!newEmail.equals(user.getEmail())) {
            Optional<User> existing = userRepository.findByEmail(newEmail);
            if (existing.isPresent()) {
                response.put("success", false);
                response.put("message", "Email already in use");
                return response;
            }
        }

        user.setName(body.get("name"));
        user.setEmail(newEmail);
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Profile updated successfully!");
        return response;
    }

    // ✅ Change password
    @PutMapping("/password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body,
                                                Authentication authentication) {
        String username = authentication.getName();
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }

        User user = userOpt.get();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            response.put("success", false);
            response.put("message", "Current password is incorrect");
            return response;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Password changed successfully!");
        return response;
    }
}