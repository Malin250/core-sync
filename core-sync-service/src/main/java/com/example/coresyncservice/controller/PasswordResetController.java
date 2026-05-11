package com.example.coresyncservice.controller;

import com.example.coresyncservice.dto.PasswordResetConfirm;
import com.example.coresyncservice.dto.PasswordResetRequest;
import com.example.coresyncservice.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.createPasswordResetToken(request.getEmail());
        return ResponseEntity.ok("Password reset token requested. Check your email (or console for now).");
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}
