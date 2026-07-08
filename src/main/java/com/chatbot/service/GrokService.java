package com.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GrokService {

    @Value("${groq.api.key}")
    private String apiKey;

    // Singleton HttpClient — created once, reused on every request
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    // ✅ NEW — wrapper record that holds both the answer AND tokens used
    public record GrokResponse(String answer, int tokensUsed) {}

    // ✅ Return type changed from String to GrokResponse
    public GrokResponse qa(List<Map<String, String>> messages) throws Exception {
        JSONArray messagesArray = new JSONArray();

        // System prompt — LocalDate called once and stored
        LocalDate today = LocalDate.now();
        messagesArray.put(new JSONObject()
                .put("role", "system")
                .put("content", "You are a helpful AI assistant. Today is "
                        + today + " (" + today.getDayOfWeek() + "). "
                        + "Answer questions clearly and accurately."));

        for (Map<String, String> msg : messages) {
            messagesArray.put(new JSONObject()
                    .put("role", msg.get("role"))
                    .put("content", msg.get("content")));
        }

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("messages", messagesArray)
                .put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofByteArray());
        String responseBody = new String(response.body(), StandardCharsets.UTF_8);

        JSONObject obj = new JSONObject(responseBody);

        if (!obj.has("choices")) {
            log.error("Groq API returned no choices: {}", responseBody);
            // ✅ Return 0 tokens on error so user isn't penalized for API failures
            return new GrokResponse("Sorry, I couldn't process that. Please try again.", 0);
        }

        String answer = obj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .replace("\u00A0", " ")
                .replace("\u2011", "-")
                .replace("\uFFFD", "")
                .trim();

        // ✅ Extract actual tokens used from Groq's usage field
        int tokensUsed = 0;
        if (obj.has("usage")) {
            tokensUsed = obj.getJSONObject("usage").optInt("total_tokens", 0);
            log.info("Groq tokens used: {}", tokensUsed);
        }

        return new GrokResponse(answer, tokensUsed);
    }
}