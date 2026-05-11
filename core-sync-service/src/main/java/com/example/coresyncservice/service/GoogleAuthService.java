package com.example.coresyncservice.service;

import com.example.coresyncservice.dto.AuthenticationResponse;
import com.example.coresyncservice.dto.GoogleAuthRequest;
import com.example.coresyncservice.exception.InvalidTokenException;
import com.example.coresyncservice.model.Role;
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies a Firebase/Google ID token and returns internal JWTs.
 *
 * <p>If the Google account is seen for the first time a new {@link User}
 * record is created (password field left empty — the account can only be
 * accessed via Google Sign-In).
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UserRepository userRepository;
    private final JwtService     jwtService;

    /**
     * Exchanges a Firebase ID token for internal JWTs.
     *
     * @param request contains the raw Firebase ID token from the Android client
     * @return {@link AuthenticationResponse} with access + refresh tokens
     * @throws InvalidTokenException if Firebase rejects the token
     */
    @Transactional
    public AuthenticationResponse exchangeGoogleToken(GoogleAuthRequest request) {
        FirebaseToken decoded;
        try {
            decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
        } catch (FirebaseAuthException ex) {
            log.warn("Google ID token verification failed: {}", ex.getMessage());
            throw new InvalidTokenException("Google ID token is invalid or expired.");
        }

        String email = decoded.getEmail();
        String name  = decoded.getName() != null ? decoded.getName() : email;

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            // No password — Google-only accounts cannot use password login
                            .password("")
                            .role(Role.USER)
                            .build();
                    log.info("Creating new user from Google Sign-In: {}", email);
                    return userRepository.save(newUser);
                });

        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
