package com.example.demo.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User Story 3: Disk Space Monitoring
 *
 * Tests verify health endpoint correctly reports disk space status.
 */
class DiskSpaceHealthCheckTest extends ActuatorHealthTestBase {

    @Value("${management.health.diskspace.threshold:10485760}")
    private long diskSpaceThreshold;

    /**
     * T015: Test disk space indicator included in health response
     * (with show-details=always in test profile)
     */
    @Test
    void healthEndpoint_withDetails_includesDiskSpaceIndicator() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.diskSpace", notNullValue()))
                .andExpect(jsonPath("$.components.diskSpace.status", is("UP")));
    }

    /**
     * T016: Test readiness probe includes disk space status at /actuator/health/readiness
     */
    @Test
    void readinessEndpoint_includesDiskSpaceStatus() throws Exception {
        mockMvc.perform(get(READINESS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.diskSpace", notNullValue()))
                .andExpect(jsonPath("$.components.diskSpace.status", is("UP")));
    }

    /**
     * T017: Verify disk space threshold configuration (10MB) is applied
     */
    @Test
    void diskSpaceIndicator_hasConfiguredThreshold() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.diskSpace.details.threshold", is(10485760)));
    }
}
