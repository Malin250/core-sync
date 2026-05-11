package com.example.coresyncservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned on successful login or registration.
 *
 * <p>Includes the short-lived access token and a longer-lived refresh token.
 * The client should store both securely (e.g. in encrypted shared preferences
 * on Android — NOT in plain SharedPreferences).
 *
 * <p>Field names use snake_case to match Android @SerializedName annotations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    /** Short-lived JWT access token (default 24 h). */
    @JsonProperty("access_token")
    private String accessToken;

    /** Longer-lived refresh token (default 7 days). */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** Display name of the authenticated user. */
    private String name;

    /** Email of the authenticated user. */
    private String email;
}
