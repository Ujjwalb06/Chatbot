package com.chatbot.controller;

import com.chatbot.dto.request.ForgotPasswordRequest;
import com.chatbot.dto.request.ResetPasswordRequest;
import com.chatbot.dto.response.ApiResponse;
import com.chatbot.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        passwordResetService.sendResetLink(req.getEmail());
        // Always same response — prevents email enumeration
        return ResponseEntity.ok(ApiResponse.ok("If that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully! You can now log in."));
    }
}