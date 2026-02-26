package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        RateLimitProperties properties = new RateLimitProperties();

        assertEquals(60, properties.getRequestsPerMinute());
        assertEquals(60000, properties.getWindowDurationMs());
        assertTrue(properties.isEnabled());
    }

    @Test
    void shouldSetCustomValues() {
        RateLimitProperties properties = new RateLimitProperties();

        properties.setRequestsPerMinute(100);
        properties.setWindowDurationMs(30000);
        properties.setEnabled(false);

        assertEquals(100, properties.getRequestsPerMinute());
        assertEquals(30000, properties.getWindowDurationMs());
        assertFalse(properties.isEnabled());
    }
}

@SpringBootTest
@TestPropertySource(properties = {
    "ratelimit.requests-per-minute=30",
    "ratelimit.window-duration-ms=30000",
    "ratelimit.enabled=true"
})
class RateLimitPropertiesConfigurationTest {

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Test
    void shouldLoadCustomConfiguration() {
        assertEquals(30, rateLimitProperties.getRequestsPerMinute());
        assertEquals(30000, rateLimitProperties.getWindowDurationMs());
        assertTrue(rateLimitProperties.isEnabled());
    }
}
