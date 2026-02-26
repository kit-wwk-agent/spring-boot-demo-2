package com.example.demo.ratelimit;

import com.example.demo.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setRequestsPerMinute(60);
        properties.setWindowDurationMs(60000);
        properties.setEnabled(true);
        rateLimitService = new RateLimitService(properties);
    }

    @Test
    void shouldIncrementCountForNewClient() {
        String clientIp = "192.168.1.1";

        boolean allowed = rateLimitService.isAllowed(clientIp);

        assertTrue(allowed);
        assertEquals(1, rateLimitService.getRequestCount(clientIp));
    }

    @Test
    void shouldIncrementCountForExistingClient() {
        String clientIp = "192.168.1.1";

        rateLimitService.isAllowed(clientIp);
        rateLimitService.isAllowed(clientIp);
        rateLimitService.isAllowed(clientIp);

        assertEquals(3, rateLimitService.getRequestCount(clientIp));
    }

    @Test
    void shouldRejectRequestWhenLimitExceeded() {
        String clientIp = "192.168.1.1";
        properties.setRequestsPerMinute(5);

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.isAllowed(clientIp));
        }

        assertFalse(rateLimitService.isAllowed(clientIp));
    }

    @Test
    void shouldTrackMultipleClientsSeparately() {
        String client1 = "192.168.1.1";
        String client2 = "192.168.1.2";
        properties.setRequestsPerMinute(3);

        rateLimitService.isAllowed(client1);
        rateLimitService.isAllowed(client1);
        rateLimitService.isAllowed(client2);

        assertEquals(2, rateLimitService.getRequestCount(client1));
        assertEquals(1, rateLimitService.getRequestCount(client2));
    }

    @Test
    void shouldResetCounterWhenWindowExpires() throws InterruptedException {
        String clientIp = "192.168.1.1";
        properties.setRequestsPerMinute(2);
        properties.setWindowDurationMs(100);

        rateLimitService.isAllowed(clientIp);
        rateLimitService.isAllowed(clientIp);
        assertFalse(rateLimitService.isAllowed(clientIp));

        Thread.sleep(150);

        assertTrue(rateLimitService.isAllowed(clientIp));
        assertEquals(1, rateLimitService.getRequestCount(clientIp));
    }

    @Test
    void shouldReturnRetryAfterSeconds() {
        String clientIp = "192.168.1.1";
        properties.setRequestsPerMinute(1);

        rateLimitService.isAllowed(clientIp);
        rateLimitService.isAllowed(clientIp);

        long retryAfter = rateLimitService.getRetryAfterSeconds(clientIp);
        assertTrue(retryAfter > 0 && retryAfter <= 60);
    }

    @Test
    void shouldAllowRequestsWhenDisabled() {
        String clientIp = "192.168.1.1";
        properties.setRequestsPerMinute(1);
        properties.setEnabled(false);

        assertTrue(rateLimitService.isAllowed(clientIp));
        assertTrue(rateLimitService.isAllowed(clientIp));
        assertTrue(rateLimitService.isAllowed(clientIp));
    }
}
