package com.chatbot.controller;

import com.chatbot.dto.request.ChangePasswordRequest;
import com.chatbot.dto.request.ProfileUpdateRequest;
import com.chatbot.dto.response.ApiResponse;
import com.chatbot.dto.response.ProfileResponse;
import com.chatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched.", userService.getProfile(auth.getName())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(@RequestBody ProfileUpdateRequest req, Authentication auth) {
        userService.updateProfile(auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully!"));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest req, Authentication auth) {
        userService.changePassword(auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully!"));
    }
}