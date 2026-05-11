package com.example.coresyncservice.controller;

import com.example.coresyncservice.config.annotations.RateLimited;
import com.example.coresyncservice.dto.AuthenticationRequest;
import com.example.coresyncservice.dto.AuthenticationResponse;
import com.example.coresyncservice.dto.GoogleAuthRequest;
import com.example.coresyncservice.dto.RegisterRequest;
import com.example.coresyncservice.service.AuthenticationService;
import com.example.coresyncservice.service.GoogleAuthService;
import com.example.coresyncservice.service.JwtService;
import com.example.coresyncservice.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user registration, login, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService  authService;
    private final GoogleAuthService      googleAuthService;
    private final JwtService             jwtService;
    private final TokenBlacklistService  blacklistService;

    // ── Google Sign-In ────────────────────────────────────────

    /**
     * Accepts a Firebase ID token from the Android Google Sign-In flow,
     * verifies it server-side, and returns internal JWTs.
     */
    @RateLimited(key = "google_auth", limit = 10, durationInSeconds = 60)
    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> googleSignIn(
            @Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.exchangeGoogleToken(request));
    }

    // ── Register ──────────────────────────────────────────────

    @RateLimited(key = "register", limit = 5, durationInSeconds = 3600)
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ── Authenticate (login) ──────────────────────────────────

    /** 10 login attempts per minute per IP. */
    @RateLimited(key = "login", limit = 10, durationInSeconds = 60)
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    // ── Refresh token ─────────────────────────────────────────

    /**
     * Exchanges a valid refresh token for a new access token.
     * The refresh token must be sent in the Authorization: Bearer header.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String refreshToken = authHeader.substring(7);
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    // ── Logout ────────────────────────────────────────────────

    /**
     * Invalidates the supplied access token by adding it to the in-memory
     * blacklist. The client must discard both the access and refresh tokens.
     *
     * <p>A 200 OK is always returned — whether the header was present or not —
     * to avoid leaking information about the token's validity.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token    = authHeader.substring(7);
            long   expiryMs = jwtService.extractExpirationMs(token);
            blacklistService.blacklist(token, expiryMs);
        }
        return ResponseEntity.ok("Logged out successfully.");
    }
}
