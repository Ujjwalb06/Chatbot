package com.chatbot.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GrokService {

    @Value("${groq.api.key}")
    private String apiKey;

    public String qa(List<Map<String, String>> messages) throws Exception {

        // Build messages JSON array from history
        JSONArray messagesArray = new JSONArray();

        //  system prompt with current date
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a helpful assistant. Today's date is "
        + java.time.LocalDate.now() + ", "
        + java.time.LocalDate.now().getDayOfWeek() + ". "
        + "Answer all questions directly and confidently.");
      messagesArray.put(systemMsg);
        for (Map<String, String> msg : messages) {
            JSONObject m = new JSONObject();
            m.put("role", msg.get("role"));
            m.put("content", msg.get("content"));
            messagesArray.put(m);
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "llama-3.3-70b-versatile");
        requestBody.put("messages", messagesArray);
        requestBody.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<byte[]> response =
                client.send(request, HttpResponse.BodyHandlers.ofByteArray());

       String responseBody = new String(response.body(), StandardCharsets.UTF_8);


       JSONObject obj = new JSONObject(responseBody);

        if (!obj.has("choices")) {
          
            return "Sorry, I couldn't process that. Please try again.";
        }

        String answer = obj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        answer = answer
                .replace("\u00A0", " ")
                .replace("\u2011", "-")
                .replace("\uFFFD", "")
                .trim();

        return answer;
    }
}