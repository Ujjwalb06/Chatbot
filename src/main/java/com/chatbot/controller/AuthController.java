package com.chatbot.controller;

import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.util.JwtUtil;
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
    public ResponseEntityWrapper register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return new ResponseEntityWrapper("Username already exists!", false);
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return new ResponseEntityWrapper("Email already registered!", false);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");  // default role
        userRepository.save(user);

        return new ResponseEntityWrapper("Registered successfully!", true);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();

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
            response.put("message", "Invalid credentials!");
        }

        return response;
    }

    // simple wrapper class for register response
    static class ResponseEntityWrapper {
        public String message;
        public boolean success;

        public ResponseEntityWrapper(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }
}