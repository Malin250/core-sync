package com.example.coresyncservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of invalidated JWTs (e.g. after logout).
 *
 * <p>Tokens are stored alongside their expiry time (epoch ms) so that the
 * nightly cleanup task can remove entries that no longer need guarding —
 * an expired token is already rejected by JWT signature validation.
 *
 * <p><b>ACTION REQUIRED (production / multi-node):</b> Replace this
 * in-memory implementation with a distributed cache (e.g. Redis) so that
 * logged-out tokens are recognised by every pod in the cluster.
 */
@Service
public class TokenBlacklistService {

    /** key = raw JWT string, value = expiry epoch-ms. */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * Adds a token to the blacklist until its natural expiry time.
     *
     * @param token     the raw JWT string
     * @param expiryMs  expiry time as epoch milliseconds
     */
    public void blacklist(String token, long expiryMs) {
        blacklist.put(token, expiryMs);
    }

    /** Returns {@code true} if the token has been explicitly invalidated. */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Removes tokens whose expiry has already passed — they are harmless
     * because JWT validation will reject them anyway.
     * Runs every night at 02:00.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(e -> e.getValue() < now);
    }
}
