package com.example.coresyncservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A short-lived token used to authorise a password-reset request.
 * Tokens expire after {@value #EXPIRY_MINUTES} minutes and are deleted
 * from the database on use or on the nightly cleanup job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    private static final int EXPIRY_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken(String token, User user) {
        this.token      = token;
        this.user       = user;
        this.expiryDate = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);
    }

    /** Returns {@code true} if this token is past its expiry time. */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
