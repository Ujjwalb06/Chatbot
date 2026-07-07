package com.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class ProfileResponse {
    private String name;
    private String email;
    private String username;
    private String role;
}