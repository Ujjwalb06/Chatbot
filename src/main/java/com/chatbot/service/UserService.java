package com.chatbot.service;

import com.chatbot.dto.request.ChangePasswordRequest;
import com.chatbot.dto.request.ProfileUpdateRequest;
import com.chatbot.dto.response.ProfileResponse;
import com.chatbot.exception.DuplicateResourceException;
import com.chatbot.exception.ResourceNotFoundException;
import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import com.chatbot.util.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ===== PROFILE =====

    public ProfileResponse getProfile(String username) {
        User user = findByUsernameOrThrow(username);
        return new ProfileResponse(user.getName(), user.getEmail(), user.getUsername(), user.getRole());
    }

    @Transactional
    public void updateProfile(String username, ProfileUpdateRequest req) {
        Validator.requireNonBlank(req.getName(), "Name");
        Validator.validateEmail(req.getEmail());

        User user = findByUsernameOrThrow(username);

        // Check email not taken by another user
        if (!req.getEmail().equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(req.getEmail()).ifPresent(existing -> {
                throw new DuplicateResourceException("Email is already in use.");
            });
        }

        user.setName(req.getName().trim());
        user.setEmail(req.getEmail().toLowerCase().trim());
        userRepository.save(user);
        log.info("Profile updated for: {}", username);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        Validator.requireNonBlank(req.getCurrentPassword(), "Current password");
        Validator.validatePassword(req.getNewPassword());

        User user = findByUsernameOrThrow(username);

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for: {}", username);
    }

    // ===== ADMIN =====

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null)); // never expose hashed password
        return users;
    }

    public User getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setPassword(null);
        return user;
    }

    @Transactional
    public void updateUser(Long id, User updated) {
        Validator.requireNonBlank(updated.getName(), "Name");
        Validator.validateEmail(updated.getEmail());

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check email not taken by another user
        if (!updated.getEmail().equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(updated.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(id))
                    throw new DuplicateResourceException("Email is already in use.");
            });
        }

        user.setName(updated.getName());
        user.setEmail(updated.getEmail());
        user.setRole(updated.getRole());
        userRepository.save(user);
        log.info("Admin updated user id: {}", id);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("Admin deleted user id: {}", id);
    }

    private User findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}