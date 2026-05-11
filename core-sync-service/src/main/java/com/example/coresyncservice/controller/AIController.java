package com.example.coresyncservice.controller;

import com.example.coresyncservice.config.annotations.RateLimited; // Import RateLimited annotation
import com.example.coresyncservice.dto.AIChatRequest;
import com.example.coresyncservice.dto.AIChatResponse;
import com.example.coresyncservice.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @RateLimited(key = "ai_chat", limit = 5, durationInSeconds = 60) // Apply rate limiting
    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chatWithAI(@Valid @RequestBody AIChatRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        AIChatResponse response = aiService.getAIResponse(userEmail, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AIChatResponse>> getChatHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        List<AIChatResponse> history = aiService.getChatHistory(userEmail);
        return ResponseEntity.ok(history);
    }
}
