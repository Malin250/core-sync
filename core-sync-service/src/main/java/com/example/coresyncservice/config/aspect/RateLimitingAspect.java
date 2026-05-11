package com.example.coresyncservice.config.aspect;

import com.example.coresyncservice.config.annotations.RateLimited;
import com.example.coresyncservice.exception.RateLimitExceededException;
import com.example.coresyncservice.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * AOP advice that enforces {@link RateLimited} on annotated controller methods.
 *
 * <p>The bucket key is built from the caller's IP address combined with the
 * annotation's logical key, so each endpoint is tracked independently per IP.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitingAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingAspect.class);

    private final RateLimitingService rateLimitingService;

    @Around("@annotation(com.example.coresyncservice.config.annotations.RateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature   = (MethodSignature) joinPoint.getSignature();
        Method          method      = signature.getMethod();
        RateLimited     rateLimited = method.getAnnotation(RateLimited.class);

        String clientIp = resolveClientIp();
        String bucketKey = clientIp + ":" + rateLimited.key();

        Bucket           bucket = rateLimitingService.resolveBucket(
                bucketKey, rateLimited.limit(), rateLimited.durationInSeconds());
        ConsumptionProbe probe  = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return joinPoint.proceed();
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
        log.warn("Rate limit exceeded for key='{}' (retry after {}s)", bucketKey, retryAfterSeconds);
        throw new RateLimitExceededException(
                String.format(
                        "Rate limit exceeded. You can retry in %d second(s).",
                        retryAfterSeconds));
    }

    /**
     * Extracts the real client IP, honouring {@code X-Forwarded-For} when the
     * service sits behind a reverse proxy or load balancer.
     *
     * <p>Only the first (leftmost) address in the header is trusted, which is
     * the original client IP when the proxy is correctly configured.
     */
    private String resolveClientIp() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
