package com.example.coresyncservice.controller;

import com.example.coresyncservice.dto.AuthenticationRequest;
import com.example.coresyncservice.dto.RegisterRequest;
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use a test profile if you have one for in-memory DB etc.
@Transactional // Rollback changes after each test
class AuthenticationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clear users before each test to ensure a clean state
        userRepository.deleteAll();
    }

    @Test
    void register_shouldRegisterUserAndReturnToken() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .profilePictureUrl("http://example.com/pic.jpg")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()));

        // Verify user is saved in DB
        User registeredUser = userRepository.findByEmail("test@example.com").orElse(null);
        assert registeredUser != null;
        assert registeredUser.getName().equals("Test User");
        assert passwordEncoder.matches("password123", registeredUser.getPassword());
    }

    @Test
    void register_shouldReturnBadRequestForInvalidEmail() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Test User")
                .email("invalid-email") // Invalid email
                .password("password123")
                .profilePictureUrl("http://example.com/pic.jpg")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.messages.email").value("Email should be valid"));
    }

    @Test
    void authenticate_shouldAuthenticateUserAndReturnToken() throws Exception {
        // First, register a user
        User user = User.builder()
                .name("Auth User")
                .email("auth@example.com")
                .password(passwordEncoder.encode("authPassword123"))
                .profilePictureUrl("http://example.com/auth.jpg")
                .build();
        userRepository.save(user);

        AuthenticationRequest authenticationRequest = AuthenticationRequest.builder()
                .email("auth@example.com")
                .password("authPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()));
    }

    @Test
    void authenticate_shouldReturnUnauthorizedForBadCredentials() throws Exception {
        // First, register a user
        User user = User.builder()
                .name("Auth User")
                .email("auth@example.com")
                .password(passwordEncoder.encode("authPassword123"))
                .profilePictureUrl("http://example.com/auth.jpg")
                .build();
        userRepository.save(user);

        AuthenticationRequest authenticationRequest = AuthenticationRequest.builder()
                .email("auth@example.com")
                .password("wrongPassword") // Wrong password
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(status().isUnauthorized()); // Spring Security returns 401 for bad credentials
    }

    @Test
    void authenticate_shouldReturnBadRequestForMissingPassword() throws Exception {
        AuthenticationRequest authenticationRequest = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("") // Missing password
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.messages.password").value("Password cannot be empty"));
    }
}
