package com.example.backend.service;

import com.example.backend.controller.AiQuestionController.GenerateQuestionsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiQuestionService {
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> requestTimes = new ConcurrentHashMap<>();

    public AiQuestionService(ObjectMapper objectMapper,
                             @Value("${app.openai.api-key:}") String apiKey,
                             @Value("${app.openai.model:gpt-4.1-mini}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Map<String, Object> generate(GenerateQuestionsRequest request, String clientAddress) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI is not configured yet. Add OPENAI_API_KEY to the backend environment.");
        }
        String topic = request == null || request.topic() == null ? "" : request.topic().trim();
        String course = request == null || request.course() == null ? "General" : request.course().trim();
        String difficulty = request == null || request.difficulty() == null ? "Intermediate" : request.difficulty().trim();
        int count = request == null || request.count() == null ? 5 : request.count();
        if (topic.isBlank() || topic.length() > 400) {
            throw new IllegalArgumentException("Enter a topic between 1 and 400 characters.");
        }
        if (count < 1 || count > 10) {
            throw new IllegalArgumentException("Choose between 1 and 10 questions.");
        }
        if (!List.of("Beginner", "Intermediate", "Advanced").contains(difficulty)) {
            throw new IllegalArgumentException("Choose Beginner, Intermediate, or Advanced difficulty.");
        }
        if (course.length() > 100) {
            throw new IllegalArgumentException("Course name must be 100 characters or less.");
        }
        enforceRateLimit(clientAddress == null ? "unknown" : clientAddress);

        Map<String, Object> questionSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "questions", Map.of("type", "array", "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "prompt", Map.of("type", "string"),
                                        "answer", Map.of("type", "string")
                                ),
                                "required", List.of("prompt", "answer"),
                                "additionalProperties", false
                        ))
                ),
                "required", List.of("questions"),
                "additionalProperties", false
        );
        String teachingPrompt = "Create exactly " + count + " original assessment questions for course '" + course
                + "' on topic '" + topic + "'. Difficulty: " + difficulty
                + ". Provide a concise correct answer for each. Keep questions clear, educational, and self-contained.";
        Map<String, Object> payload = Map.of(
                "model", model,
                "input", List.of(Map.of("role", "user", "content", teachingPrompt)),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "generated_questions",
                        "strict", true,
                        "schema", questionSchema
                )),
                "max_output_tokens", 1800
        );

        String responseBody = restClient.post()
                .uri("https://api.openai.com/v1/responses")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            String outputText = null;
            for (JsonNode output : response.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        outputText = content.path("text").asText();
                        break;
                    }
                }
                if (outputText != null) break;
            }
            if (outputText == null) throw new IllegalStateException("AI returned no question content.");
            JsonNode generated = objectMapper.readTree(outputText).path("questions");
            if (!generated.isArray() || generated.isEmpty()) throw new IllegalStateException("AI returned no questions.");
            return Map.of("questions", objectMapper.convertValue(generated, List.class));
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read the AI question response.", exception);
        }
    }

    private void enforceRateLimit(String address) {
        ArrayDeque<Instant> times = requestTimes.computeIfAbsent(address, ignored -> new ArrayDeque<>());
        synchronized (times) {
            Instant cutoff = Instant.now().minusSeconds(60);
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) times.removeFirst();
            if (times.size() >= MAX_REQUESTS_PER_MINUTE) {
                throw new IllegalArgumentException("Too many requests. Please wait a minute and try again.");
            }
            times.addLast(Instant.now());
        }
    }
}
