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
    "ratelimit.requests-per-minute=2",
    "ratelimit.window-duration-ms=60000",
    "ratelimit.enabled=true"
})
class RateLimitExclusionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldNotRateLimitActuatorHealth() throws Exception {
        String testIp = "172.16.0.1";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/test")
                    .with(request -> {
                        request.setRemoteAddr(testIp);
                        return request;
                    }))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/actuator/health")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isOk());
    }

    @Test
    void shouldNotRateLimitActuatorInfo() throws Exception {
        String testIp = "172.16.0.2";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/test")
                    .with(request -> {
                        request.setRemoteAddr(testIp);
                        return request;
                    }))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/actuator/info")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAlwaysAllowActuatorEvenWhenRateLimited() throws Exception {
        String testIp = "172.16.0.3";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/actuator/health")
                    .with(request -> {
                        request.setRemoteAddr(testIp);
                        return request;
                    }))
                .andExpect(status().isOk());
        }
    }

    @Test
    void shouldRateLimitApiButNotActuator() throws Exception {
        String testIp = "172.16.0.4";

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/test")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/actuator/health")
                .with(request -> {
                    request.setRemoteAddr(testIp);
                    return request;
                }))
            .andExpect(status().isOk());
    }
}
