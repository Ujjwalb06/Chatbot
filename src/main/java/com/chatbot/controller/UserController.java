package com.chatbot.controller;

import com.chatbot.model.User;
import com.chatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // ✅ Get all users (password excluded manually)
    @GetMapping
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        // hide passwords before sending to frontend
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    // ✅ Get single user by id
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            user.get().setPassword(null);
            return user.get();
        }
        return null;
    }

    // ✅ Update user
    @PutMapping("/{id}")
    public String updateUser(@PathVariable Long id, @RequestBody User updated) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) return "User not found!";

        User user = existing.get();
        user.setName(updated.getName());
        user.setEmail(updated.getEmail());
        user.setUsername(updated.getUsername());
        user.setRole(updated.getRole());
        // password unchanged unless explicitly handled separately

        userRepository.save(user);
        return "User updated successfully!";
    }

    // ✅ Delete user
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) return "User not found!";
        userRepository.deleteById(id);
        return "User deleted successfully!";
    }
}