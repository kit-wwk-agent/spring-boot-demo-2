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
    "ratelimit.requests-per-minute=3",
    "ratelimit.window-duration-ms=60000",
    "ratelimit.enabled=true"
})
class RateLimitConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldApplyCustomLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/test")
                    .with(request -> {
                        request.setRemoteAddr("192.168.100.1");
                        return request;
                    }))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr("192.168.100.1");
                    return request;
                }))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldWorkWithLowerLimit() throws Exception {
        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr("192.168.100.2");
                    return request;
                }))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr("192.168.100.2");
                    return request;
                }))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr("192.168.100.2");
                    return request;
                }))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr("192.168.100.2");
                    return request;
                }))
            .andExpect(status().isTooManyRequests());
    }
}
