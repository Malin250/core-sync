package com.example.coresyncservice.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {

    // Cache to store buckets for different keys (e.g., IP addresses, user IDs, endpoint names)
    private final LoadingCache<String, Bucket> buckets;

    public RateLimitingService() {
        // Initialize the cache with a maximum size and expiry
        buckets = CacheBuilder.newBuilder()
                .maximumSize(10000) // Max 10,000 buckets in cache
                .expireAfterAccess(1, TimeUnit.HOURS) // Buckets expire after 1 hour of inactivity
                .build(new CacheLoader<>() {
                    @Override
                    public Bucket load(String key) {
                        // Default bucket configuration if not specified elsewhere
                        // This will be overridden by @RateLimited annotation
                        return Bucket.builder()
                                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1))) // 10 requests per minute by default
                                .build();
                    }
                });
    }

    public Bucket resolveBucket(String key, int limit, long durationInSeconds) {
        // Configure the refill and capacity based on the annotation
        Bandwidth bandwidth = Bandwidth.builder().capacity(limit).refillGreedy(limit, Duration.ofSeconds(durationInSeconds)).build();

        // Get or create a bucket for the given key
        // The computeIfAbsent method ensures that only one bucket is created for each key
        try {
            return buckets.get(key, () -> Bucket.builder().addLimit(bandwidth).build());
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to resolve rate limit bucket for key: " + key, e);
        }
    }
}
