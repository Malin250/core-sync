package com.example.coresyncservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for the Google ID-token exchange endpoint.
 * The client sends the ID token it received from Firebase/Google Sign-In.
 */
@Data
@NoArgsConstructor
public class GoogleAuthRequest {

    /** The Firebase/Google ID token obtained from the Android client. */
    @JsonProperty("id_token")
    @NotBlank(message = "id_token must not be blank")
    private String idToken;
}
