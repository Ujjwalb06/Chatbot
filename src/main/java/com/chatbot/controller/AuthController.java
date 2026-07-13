package com.chatbot.controller;

import com.chatbot.dto.request.LoginRequest;
import com.chatbot.dto.request.RegisterRequest;
import com.chatbot.dto.response.ApiResponse;
import com.chatbot.dto.response.LoginResponse;
import com.chatbot.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registered successfully!"));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        // ✅ Returns flat response — same format frontend already expects
        // { success, token, username, name, role }
        LoginResponse data = authService.login(req);
        String datoken=data.getToken();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", data.getToken());
        response.put("username", data.getUsername());
        response.put("name", data.getName());
        response.put("role", data.getRole());
        return response;
    }
}