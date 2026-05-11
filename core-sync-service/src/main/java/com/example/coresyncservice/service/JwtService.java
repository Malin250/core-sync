package com.example.coresyncservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles JWT creation, validation, and claim extraction.
 *
 * <p>The secret key is read from the environment variable
 * {@code JWT_SECRET_KEY} (via {@code application.properties}).  It must be a
 * hex-encoded 256-bit (32-byte) random value — generate one with
 * {@code openssl rand -hex 32}.
 */
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${application.security.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    // ── Token generation ──────────────────────────────────────

    /** Generates a short-lived access token for the given user. */
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, accessTokenExpiration);
    }

    /** Generates a longer-lived refresh token for the given user. */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> extra = Map.of("type", "refresh");
        return buildToken(extra, userDetails, refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token validation ──────────────────────────────────────

    /** Returns {@code true} if the token is cryptographically valid and not expired. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // ── Claim extraction ──────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Returns the expiry of the token as epoch milliseconds (useful for blacklisting). */
    public long extractExpirationMs(String token) {
        return extractClaim(token, Claims::getExpiration).getTime();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    // ── Internals ─────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Returns {@code true} if the token carries the {@code "type":"refresh"} claim. */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, claims -> claims.get("type", String.class)));
    }
}
