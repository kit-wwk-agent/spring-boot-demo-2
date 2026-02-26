package com.example.demo.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the number of requests from a specific client IP within the current time window.
 */
public class RateLimitCounter {

    private final String clientIp;
    private final AtomicInteger requestCount;
    private volatile long windowStartTime;

    public RateLimitCounter(String clientIp, long windowStartTime) {
        this.clientIp = clientIp;
        this.requestCount = new AtomicInteger(0);
        this.windowStartTime = windowStartTime;
    }

    /**
     * Atomically increment the request count.
     * @return the new count after incrementing
     */
    public int increment() {
        return requestCount.incrementAndGet();
    }

    /**
     * Check if the current window has expired.
     * @param windowDurationMs the duration of the window in milliseconds
     * @return true if the window has expired
     */
    public boolean isExpired(long windowDurationMs) {
        return System.currentTimeMillis() - windowStartTime >= windowDurationMs;
    }

    /**
     * Reset the counter for a new window.
     * @param currentTime the current time in milliseconds
     */
    public void reset(long currentTime) {
        this.windowStartTime = currentTime;
        this.requestCount.set(0);
    }

    public String getClientIp() {
        return clientIp;
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public long getWindowStartTime() {
        return windowStartTime;
    }
}
