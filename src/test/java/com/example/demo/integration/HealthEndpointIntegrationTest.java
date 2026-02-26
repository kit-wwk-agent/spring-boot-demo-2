package com.example.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ========================================
    // User Story 1: Basic Health Check for Kubernetes Probes
    // ========================================

    @Test
    void healthEndpoint_whenApplicationIsHealthy_shouldReturnHttp200WithStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthEndpoint_shouldRespondWithinPerformanceThreshold() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        long responseTime = System.currentTimeMillis() - startTime;

        // Response time should be under 500ms as per FR-006
        org.junit.jupiter.api.Assertions.assertTrue(
            responseTime < 500,
            "Health endpoint response time (" + responseTime + "ms) exceeded 500ms threshold"
        );
    }

    @Test
    void livenessEndpoint_shouldReturnHttp200WithStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessEndpoint_shouldReturnHttp200WithStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // ========================================
    // User Story 2: Structured Health Data for Monitoring Systems
    // ========================================

    @Test
    void healthEndpoint_shouldIncludeDatabaseHealthIndicator() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.db").exists())
            .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void healthEndpoint_shouldIncludeDiskSpaceHealthIndicator() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.diskSpace").exists())
            .andExpect(jsonPath("$.components.diskSpace.status").value("UP"));
    }

    @Test
    void healthEndpoint_shouldIncludeDatabaseDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.db.details.database").exists());
    }

    @Test
    void healthEndpoint_shouldIncludeDiskSpaceDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.diskSpace.details.total").exists())
            .andExpect(jsonPath("$.components.diskSpace.details.free").exists())
            .andExpect(jsonPath("$.components.diskSpace.details.threshold").exists());
    }

    @Test
    void readinessEndpoint_shouldIncludeDatabaseHealth() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.db").exists())
            .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void readinessEndpoint_shouldIncludeDiskSpaceHealth() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.diskSpace").exists())
            .andExpect(jsonPath("$.components.diskSpace.status").value("UP"));
    }

    // ========================================
    // User Story 3: Unauthenticated Access for Infrastructure Tools
    // ========================================

    @Test
    void healthEndpoint_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // No authentication headers provided - should still succeed
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void livenessEndpoint_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // No authentication headers provided - should still succeed
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void readinessEndpoint_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // No authentication headers provided - should still succeed
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());
    }

    // ========================================
    // Additional Health Probe Tests
    // ========================================

    @Test
    void livenessEndpoint_shouldOnlyIncludeLivenessState() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.livenessState").exists())
            // Should NOT include db or diskSpace in liveness
            .andExpect(jsonPath("$.components.db").doesNotExist())
            .andExpect(jsonPath("$.components.diskSpace").doesNotExist());
    }

    @Test
    void readinessEndpoint_shouldIncludeReadinessState() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.readinessState").exists())
            .andExpect(jsonPath("$.components.readinessState.status").value("UP"));
    }
}
