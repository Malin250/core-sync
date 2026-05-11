package com.example.coresyncservice.service;

import com.example.coresyncservice.dto.AIChatRequest;
import com.example.coresyncservice.dto.AIChatResponse;
import com.example.coresyncservice.dto.ExternalAIRequest;
import com.example.coresyncservice.dto.ExternalAIResponse;
import com.example.coresyncservice.exception.UserNotFoundException;
import com.example.coresyncservice.model.AIChatHistory;
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.repository.AIChatHistoryRepository;
import com.example.coresyncservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Proxies chat messages to an external AI API and persists the conversation
 * history per user.
 */
@Service
@RequiredArgsConstructor
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final AIChatHistoryRepository aiChatHistoryRepository;
    private final UserRepository          userRepository;
    private final RestTemplate            restTemplate;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    @Value("${ai.api.key}")
    private String aiApiKey;

    /** AI model identifier, e.g. "gpt-3.5-turbo". */
    @Value("${ai.api.model:gpt-3.5-turbo}")
    private String aiModel;

    @Value("${ai.system-prompt}")
    private String systemPrompt;

    /** Number of past conversation turns sent to the model for context. */
    @Value("${ai.chat.history-limit:5}")
    private int chatHistoryLimit;

    // ── Public API ────────────────────────────────────────────

    /**
     * Sends the user's message to the AI and returns the response.
     * Both the request and the response are persisted to chat history.
     */
    @Transactional
    public AIChatResponse getAIResponse(String userEmail, AIChatRequest request) {
        User user = requireUser(userEmail);

        String aiResponseContent = callAiApi(user, request.getMessage());

        AIChatHistory entry = AIChatHistory.builder()
                .user(user)
                .requestContent(request.getMessage())
                .responseContent(aiResponseContent)
                .timestamp(LocalDateTime.now())
                .build();
        aiChatHistoryRepository.save(entry);

        return AIChatResponse.builder().response(aiResponseContent).build();
    }

    /** Returns the full chat history for the given user, oldest first. */
    @Transactional(readOnly = true)
    public List<AIChatResponse> getChatHistory(String userEmail) {
        User user = requireUser(userEmail);

        return aiChatHistoryRepository.findByUserOrderByTimestampAsc(user).stream()
                .map(h -> AIChatResponse.builder()
                        .response("User: " + h.getRequestContent() + "\nAI: " + h.getResponseContent())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Internals ─────────────────────────────────────────────

    private String callAiApi(User user, String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiApiKey);

            List<ExternalAIRequest.Message> messages = buildMessageList(user, userMessage);

            ExternalAIRequest requestPayload = ExternalAIRequest.builder()
                    .model(aiModel)
                    .messages(messages)
                    .build();

            ResponseEntity<ExternalAIResponse> response = restTemplate.postForEntity(
                    aiApiUrl,
                    new HttpEntity<>(requestPayload, headers),
                    ExternalAIResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getChoices() != null
                    && !response.getBody().getChoices().isEmpty()) {
                return response.getBody().getChoices().get(0).getMessage().getContent();
            }

            log.warn("AI API returned an unexpected response: status={}", response.getStatusCode());
            return "No response received from the AI service.";

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("AI API HTTP error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return "The AI service returned an error. Please try again later.";
        } catch (Exception ex) {
            log.error("Unexpected error calling AI API", ex);
            return "An unexpected error occurred. Please try again later.";
        }
    }

    /** Builds the ordered message list: system prompt → recent history → current message. */
    private List<ExternalAIRequest.Message> buildMessageList(User user, String userMessage) {
        List<ExternalAIRequest.Message> messages = new ArrayList<>();

        // 1. System prompt
        messages.add(msg("system", systemPrompt));

        // 2. Recent history (last N turns)
        List<AIChatHistory> history = aiChatHistoryRepository.findByUserOrderByTimestampAsc(user);
        if (history.size() > chatHistoryLimit) {
            history = history.subList(history.size() - chatHistoryLimit, history.size());
        }
        for (AIChatHistory h : history) {
            messages.add(msg("user",      h.getRequestContent()));
            messages.add(msg("assistant", h.getResponseContent()));
        }

        // 3. Current user message
        messages.add(msg("user", userMessage));

        return messages;
    }

    private static ExternalAIRequest.Message msg(String role, String content) {
        return ExternalAIRequest.Message.builder().role(role).content(content).build();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
    }
}
