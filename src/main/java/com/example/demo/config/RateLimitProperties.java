package com.example.demo.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for rate limiting.
 */
@ConfigurationProperties(prefix = "ratelimit")
@Validated
public class RateLimitProperties {

    /**
     * Maximum requests allowed per minute per client IP.
     */
    @Min(value = 1, message = "requests-per-minute must be at least 1")
    @Max(value = 10000, message = "requests-per-minute must not exceed 10000")
    private int requestsPerMinute = 60;

    /**
     * Rate limit window duration in milliseconds.
     */
    private long windowDurationMs = 60000;

    /**
     * Enable/disable rate limiting globally.
     */
    private boolean enabled = true;

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public long getWindowDurationMs() {
        return windowDurationMs;
    }

    public void setWindowDurationMs(long windowDurationMs) {
        this.windowDurationMs = windowDurationMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
