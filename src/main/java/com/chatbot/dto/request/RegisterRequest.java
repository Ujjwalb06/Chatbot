package com.chatbot.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String username;
    private String password;
}