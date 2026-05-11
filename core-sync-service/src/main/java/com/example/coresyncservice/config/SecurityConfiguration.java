package com.example.coresyncservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security filter-chain configuration.
 *
 * <p><b>CORS note:</b> {@code allowCredentials(true)} is incompatible with a
 * wildcard origin ("*") — browsers will reject such responses. We therefore
 * require an explicit {@code ALLOWED_ORIGINS} environment variable in
 * production. The empty-string default below is intentionally restrictive.
 *
 * <p>ACTION REQUIRED: Set {@code ALLOWED_ORIGINS} in your environment to a
 * comma-separated list of your front-end origins, e.g.
 * {@code https://app.example.com,https://admin.example.com}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize / @Secured on service methods
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider  authenticationProvider;

    /** Comma-separated allowed origins, e.g. "https://app.example.com". */
    @Value("${cors.allowed-origins:}")
    private String allowedOriginsRaw;

    // ── Endpoints that do NOT need a JWT ──────────────────────
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",          // register, login, password-reset
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CSRF: disabled — stateless JWT API ────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── CORS ──────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Authorisation rules ───────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )

                // ── Stateless session ─────────────────────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ── Security response headers ─────────────────
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'"))
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                )

                // ── JWT filter ────────────────────────────────
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        /*
         * Build the allowed-origins list from the environment variable.
         * Keeping a BLANK value is safe — it means NO cross-origin requests
         * are permitted, which is the correct default for a back-end service
         * without a known front-end yet.
         */
        List<String> origins = allowedOriginsRaw.isBlank()
                ? List.of()
                : List.of(allowedOriginsRaw.split(","));

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-Requested-With"));
        // credentials (Authorization header) requires an explicit origin list, never "*"
        config.setAllowCredentials(!origins.isEmpty());
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
