package com.example.coresyncservice.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method for IP-based rate limiting.
 *
 * <p>Usage example:
 * <pre>
 *   {@literal @}RateLimited(key = "ai_chat", limit = 5, durationInSeconds = 60)
 *   {@literal @}PostMapping("/chat")
 *   public ResponseEntity<...> chat(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    /** Logical name for this limit bucket (e.g. "login", "ai_chat"). */
    String key();
    /** Maximum number of calls allowed within the window. */
    int limit();
    /** Length of the sliding window in seconds. */
    long durationInSeconds();
}
