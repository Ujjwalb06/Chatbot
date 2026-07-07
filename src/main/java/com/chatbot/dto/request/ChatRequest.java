package com.chatbot.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ChatRequest {
    private Long sessionId;
    private List<Map<String, String>> messages;
}