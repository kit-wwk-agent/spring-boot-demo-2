package com.example.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "ratelimit.requests-per-minute=5",
    "ratelimit.window-duration-ms=60000",
    "ratelimit.enabled=true"
})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowRequestsWithinLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/test"))
                .andExpect(status().isOk());
        }
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/test")
                    .with(request -> {
                        request.setRemoteAddr("10.0.0.2");
                        return request;
                    }))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.2");
                    return request;
                }))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error").value("Too Many Requests"))
            .andExpect(jsonPath("$.retryAfter").isNumber());
    }

    @Test
    void shouldTrackDifferentClientsSeparately() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/test")
                    .with(request -> {
                        request.setRemoteAddr("10.0.0.3");
                        return request;
                    }))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.4");
                    return request;
                }))
            .andExpect(status().isOk());
    }

    @Test
    void shouldNotRateLimitActuatorEndpoints() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/actuator/health")
                    .with(request -> {
                        request.setRemoteAddr("10.0.0.5");
                        return request;
                    }))
                .andExpect(status().isOk());
        }
    }

    @Test
    void shouldExtractClientIpFromXForwardedFor() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/test")
                    .header("X-Forwarded-For", "203.0.113.1, 192.168.1.1"))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/test")
                .header("X-Forwarded-For", "203.0.113.1, 192.168.1.1"))
            .andExpect(status().isTooManyRequests());
    }
}
