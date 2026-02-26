package com.example.demo.ratelimit;

/**
 * Response body returned when rate limit is exceeded.
 */
public class RateLimitResponse {

    private final String error;
    private final String message;
    private final long retryAfter;

    public RateLimitResponse(String error, String message, long retryAfter) {
        this.error = error;
        this.message = message;
        this.retryAfter = retryAfter;
    }

    public static RateLimitResponse tooManyRequests(long retryAfterSeconds) {
        return new RateLimitResponse(
            "Too Many Requests",
            "Rate limit exceeded. Please retry after " + retryAfterSeconds + " seconds.",
            retryAfterSeconds
        );
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public long getRetryAfter() {
        return retryAfter;
    }
}
