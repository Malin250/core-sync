package com.example.coresyncservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirm {
    @NotBlank(message = "Token cannot be empty")
    private String token;

    @NotBlank(message = "New password cannot be empty")
    @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;
}
