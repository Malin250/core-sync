package com.example.coresyncservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStateDto {
    @NotBlank(message = "State JSON cannot be empty")
    private String stateJson;
}
