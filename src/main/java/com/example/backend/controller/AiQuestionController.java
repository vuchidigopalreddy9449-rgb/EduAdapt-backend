package com.example.backend.controller;

import com.example.backend.service.AiQuestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiQuestionController {
    private final AiQuestionService aiQuestionService;

    public AiQuestionController(AiQuestionService aiQuestionService) {
        this.aiQuestionService = aiQuestionService;
    }

    @PostMapping("/questions")
    public ResponseEntity<?> generateQuestions(@RequestBody GenerateQuestionsRequest request,
                                               HttpServletRequest servletRequest) {
        try {
            return ResponseEntity.ok(aiQuestionService.generate(request, servletRequest.getRemoteAddr()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(503).body(Map.of("error", exception.getMessage()));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(502).body(Map.of("error", "AI question generation failed. Please try again."));
        }
    }

    public record GenerateQuestionsRequest(String topic, String course, Integer count, String difficulty) { }
}
