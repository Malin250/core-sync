package com.example.coresyncservice.service;

import com.example.coresyncservice.exception.InvalidTokenException;
import com.example.coresyncservice.dto.AuthenticationRequest;
import com.example.coresyncservice.dto.AuthenticationResponse;
import com.example.coresyncservice.dto.RegisterRequest;
import com.example.coresyncservice.exception.UserNotFoundException;
import com.example.coresyncservice.model.Role;
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and authentication.
 *
 * <p>After {@value #MAX_FAILED_ATTEMPTS} consecutive failed login attempts the
 * account is locked and must be unlocked by an administrator or via a
 * password-reset flow.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    /** Number of failed attempts before the account is locked. */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtService          jwtService;
    private final AuthenticationManager authenticationManager;

    // ── Registration ──────────────────────────────────────────

    /**
     * Creates a new user account and returns tokens.
     *
     * @throws IllegalStateException if the email is already registered
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            // Return a vague message to avoid user-enumeration via error text
            throw new IllegalStateException("Registration failed. Please try a different email address.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profilePictureUrl(request.getProfilePictureUrl())
                .role(Role.USER)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    // ── Authentication ────────────────────────────────────────

    /**
     * Validates credentials and returns tokens.
     *
     * <p>Failed attempts increment a counter on the user record. After
     * {@value #MAX_FAILED_ATTEMPTS} failures the account is locked.
     */
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid credentials."));

        if (user.isAccountLocked()) {
            throw new LockedException("Your account has been locked. Please reset your password.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            handleFailedLogin(user);
            // Return a generic message — do NOT reveal whether the email exists
            throw new BadCredentialsException("Invalid credentials.");
        }

        // Successful login — reset the failure counter
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        log.info("User authenticated: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            log.warn("Account locked after {} failed login attempts: {}", attempts, user.getEmail());
        }

        userRepository.save(user);
    }

    private AuthenticationResponse buildAuthResponse(User user) {
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    // ── Token refresh ─────────────────────────────────────────

    /**
     * Validates a refresh token and issues a new access token.
     *
     * @param refreshToken the raw refresh JWT from the client
     * @return a new {@link AuthenticationResponse} with a fresh access token
     *         (the same refresh token is returned unchanged)
     * @throws InvalidTokenException if the token is not a refresh token,
     *         is expired, or does not belong to a known user
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Provided token is not a refresh token.");
        }

        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception ex) {
            throw new InvalidTokenException("Refresh token is invalid or expired.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("Refresh token belongs to an unknown user."));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Refresh token is invalid or expired.");
        }

        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
