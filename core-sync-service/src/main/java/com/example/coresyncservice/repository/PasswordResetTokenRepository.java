package com.example.coresyncservice.repository;

import com.example.coresyncservice.model.PasswordResetToken;
import com.example.coresyncservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);

    /** Bulk-deletes expired tokens; used by the nightly cleanup job. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    int deleteByExpiryDateBefore(@Param("now") LocalDateTime now);
}
