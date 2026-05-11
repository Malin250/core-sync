package com.example.coresyncservice.service;

import com.example.coresyncservice.dto.PasswordResetConfirm;
import com.example.coresyncservice.exception.InvalidTokenException;
import com.example.coresyncservice.exception.UserNotFoundException;
import com.example.coresyncservice.model.PasswordResetToken;
import com.example.coresyncservice.model.User;
import com.example.coresyncservice.repository.PasswordResetTokenRepository;
import com.example.coresyncservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manages password-reset token lifecycle: creation, validation, and cleanup.
 *
 * <p>ACTION REQUIRED — email delivery: Replace the {@code log.info} call in
 * {@link #createPasswordResetToken} with a real email dispatch (e.g. via
 * Spring Mail + an SMTP relay or a service like SendGrid / SES).  The token
 * must be sent ONLY to the registered email and not written to any log in
 * production.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository               userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder              passwordEncoder;

    // ── Token creation ────────────────────────────────────────

    /**
     * Issues a new password-reset token for the user with the given email.
     * Any pre-existing token for that user is invalidated first.
     */
    @Transactional
    public void createPasswordResetToken(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("No account found with that email address."));

        // Invalidate any previous token for this user
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        String token      = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        /*
         * ACTION REQUIRED: Replace the log line below with actual email
         * delivery and remove it from production configs.
         * Do NOT log real tokens in production — treat them like passwords.
         */
        log.info("[DEV ONLY] Password reset token for {}: {}", userEmail, token);
    }

    // ── Password reset ────────────────────────────────────────

    /** Validates the token and updates the user's password. */
    @Transactional
    public void resetPassword(PasswordResetConfirm request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token."));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new InvalidTokenException("Password reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Unlock the account on successful password reset
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        tokenRepository.delete(resetToken); // Single-use — invalidate immediately
        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    // ── Scheduled cleanup ─────────────────────────────────────

    /**
     * Removes expired reset tokens from the database every night at 03:00
     * to keep the table clean.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired password-reset token(s).", deleted);
        }
    }
}
