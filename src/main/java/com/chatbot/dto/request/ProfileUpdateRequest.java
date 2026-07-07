package com.chatbot.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProfileUpdateRequest {
    private String name;
    private String email;
}