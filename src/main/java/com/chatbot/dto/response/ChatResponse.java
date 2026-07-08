package com.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class ChatResponse {
    private String answer;
    private String sessionTitle;
    private boolean ragUsed;
    private int tokensRemaining; // ✅ NEW — frontend can show remaining tokens
}