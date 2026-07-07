package com.chatbot.controller;

import com.chatbot.dto.response.ApiResponse;
import com.chatbot.model.User;
import com.chatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ✅ Returns List<User> directly — same format frontend expects
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // ✅ Returns User directly — same format frontend expects
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody User updated) {
        userService.updateUser(id, updated);
        return Map.of("success", true, "message", "User updated successfully!");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Map.of("success", true, "message", "User deleted successfully!");
    }
}