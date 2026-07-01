package com.chatbot.controller;

import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.util.JwtUtil;
import com.chatbot.util.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();

        // ✅ Validate all fields before touching the DB
        Validator.requireNonBlank(user.getName(), "Name");
        Validator.validateEmail(user.getEmail());
        Validator.validateUsername(user.getUsername());
        Validator.validatePassword(user.getPassword());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            response.put("success", false);
            response.put("message", "Username already exists!");
            return response;
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            response.put("success", false);
            response.put("message", "Email already registered!");
            return response;
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Registered successfully!");
        return response;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();

        // ✅ Validate inputs before hitting DB
        Validator.requireNonBlank(user.getUsername(), "Username");
        Validator.requireNonBlank(user.getPassword(), "Password");

        Optional<User> existing = userRepository.findByUsername(user.getUsername());

        if (existing.isPresent()
                && passwordEncoder.matches(user.getPassword(), existing.get().getPassword())) {

            User u = existing.get();
            String token = jwtUtil.generateToken(u.getUsername(), u.getRole());

            response.put("success", true);
            response.put("token", token);
            response.put("username", u.getUsername());
            response.put("name", u.getName());
            response.put("role", u.getRole());
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password.");
        }

        return response;
    }
}