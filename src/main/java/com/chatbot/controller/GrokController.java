package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.service.GrokService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GrokController {

    @Autowired
    private GrokService grokService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, Object> body,
                       Authentication authentication) throws Exception {

        String username = authentication.getName();

        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");

        if (!messages.isEmpty()) {
            Map<String, String> lastUserMsg = messages.get(messages.size() - 1);
            chatMessageRepository.save(
                new ChatMessage(username, lastUserMsg.get("role"), lastUserMsg.get("content"))
            );
        }

        String answer = grokService.qa(messages);

        chatMessageRepository.save(
            new ChatMessage(username, "assistant", answer)
        );

        return answer;
    }
}