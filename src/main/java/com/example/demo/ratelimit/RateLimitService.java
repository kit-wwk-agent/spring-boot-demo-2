package com.example.demo.ratelimit;

import com.example.demo.config.RateLimitProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for rate limiting logic using fixed window algorithm.
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final ConcurrentHashMap<String, RateLimitCounter> counters = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;
    private final ScheduledExecutorService cleanupExecutor;

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 60, 60, TimeUnit.SECONDS);
        logger.info("Rate limit cleanup task scheduled to run every 60 seconds");
    }

    @PreDestroy
    public void stopCleanupTask() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Rate limit cleanup task stopped");
    }

    /**
     * Check if a request from the given client IP is allowed.
     * @param clientIp the client IP address
     * @return true if the request is allowed, false if rate limited
     */
    public boolean isAllowed(String clientIp) {
        if (!properties.isEnabled()) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long windowDurationMs = properties.getWindowDurationMs();
        int limit = properties.getRequestsPerMinute();

        RateLimitCounter counter = counters.compute(clientIp, (key, existing) -> {
            if (existing == null) {
                RateLimitCounter newCounter = new RateLimitCounter(clientIp, currentTime);
                newCounter.increment();
                return newCounter;
            }

            if (existing.isExpired(windowDurationMs)) {
                logger.debug("Rate limit window reset for client IP: {}", clientIp);
                existing.reset(currentTime);
            }

            existing.increment();
            return existing;
        });

        return counter.getRequestCount() <= limit;
    }

    /**
     * Get the number of seconds until the rate limit window resets for the given client.
     * @param clientIp the client IP address
     * @return seconds until retry is allowed
     */
    public long getRetryAfterSeconds(String clientIp) {
        RateLimitCounter counter = counters.get(clientIp);
        if (counter == null) {
            return 0;
        }

        long windowDurationMs = properties.getWindowDurationMs();
        long elapsed = System.currentTimeMillis() - counter.getWindowStartTime();
        long remainingMs = windowDurationMs - elapsed;

        return Math.max(0, (remainingMs + 999) / 1000);
    }

    /**
     * Get the current request count for a client (for testing purposes).
     * @param clientIp the client IP address
     * @return the current request count, or 0 if not tracked
     */
    public int getRequestCount(String clientIp) {
        RateLimitCounter counter = counters.get(clientIp);
        return counter != null ? counter.getRequestCount() : 0;
    }

    /**
     * Remove expired entries from the counter map (for cleanup task).
     */
    public void cleanupExpiredEntries() {
        int sizeBefore = counters.size();
        long windowDurationMs = properties.getWindowDurationMs();
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(windowDurationMs));
        int removed = sizeBefore - counters.size();
        if (removed > 0) {
            logger.debug("Cleaned up {} expired rate limit entries", removed);
        }
    }
}
