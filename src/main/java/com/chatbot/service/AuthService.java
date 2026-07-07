package com.chatbot.service;

import com.chatbot.dto.request.LoginRequest;
import com.chatbot.dto.request.RegisterRequest;
import com.chatbot.dto.response.LoginResponse;
import com.chatbot.exception.DuplicateResourceException;
import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.util.JwtUtil;
import com.chatbot.util.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void register(RegisterRequest req) {
        // Validate inputs
        Validator.requireNonBlank(req.getName(), "Name");
        Validator.validateEmail(req.getEmail());
        Validator.validateUsername(req.getUsername());
        Validator.validatePassword(req.getPassword());

        // Check duplicates — throw specific exception for correct HTTP 409 status
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists.");
        }
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already registered.");
        }

        User user = new User(
                req.getName(),
                req.getEmail(),
                req.getUsername(),
                passwordEncoder.encode(req.getPassword())
        );
        userRepository.save(user);
        log.info("New user registered: {}", req.getUsername());
    }

    public LoginResponse login(LoginRequest req) {
        Validator.requireNonBlank(req.getUsername(), "Username");
        Validator.requireNonBlank(req.getPassword(), "Password");

        User user = userRepository.findByUsername(req.getUsername())
                .filter(u -> passwordEncoder.matches(req.getPassword(), u.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        log.info("User logged in: {}", user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getName(), user.getRole());
    }
}