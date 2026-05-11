package com.example.coresyncservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    // Email is typically not updated via this DTO, but we can ensure it's a valid format if present
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @URL(message = "Profile picture URL should be a valid URL", regexp = "^(http|https)://.*$")
    private String profilePictureUrl;
}
