package com.example.demo.health;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User Story 1: Basic Health Check for Load Balancers
 *
 * Tests verify health endpoint returns correct HTTP 200 with {"status":"UP"}
 * for load balancer integration.
 */
class BasicHealthCheckTest extends ActuatorHealthTestBase {

    /**
     * T007: Test health endpoint returns HTTP 200 when healthy
     */
    @Test
    void healthEndpoint_whenHealthy_returnsHttp200() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk());
    }

    /**
     * T008: Test health endpoint returns JSON body {"status":"UP"} when healthy
     */
    @Test
    void healthEndpoint_whenHealthy_returnsStatusUp() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    /**
     * T009: Test health endpoint accessible without authentication
     */
    @Test
    void healthEndpoint_noAuthentication_isAccessible() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk());
    }

    /**
     * T010: Test health endpoint response time under 500ms
     */
    @Test
    void healthEndpoint_responseTime_underThreshold() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk());

        long responseTime = System.currentTimeMillis() - startTime;
        assert responseTime < 500 : "Health endpoint response time (" + responseTime + "ms) exceeded 500ms threshold";
    }

    /**
     * T011: Test health endpoint returns Content-Type application/json
     *
     * Spring Boot Actuator returns application/vnd.spring-boot.actuator.v3+json
     * which is a JSON-compatible vendor type.
     */
    @Test
    void healthEndpoint_contentType_isApplicationJson() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.contains("json") :
                            "Expected JSON content type but got: " + contentType;
                });
    }
}
