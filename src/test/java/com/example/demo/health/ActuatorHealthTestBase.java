package com.example.demo.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base test class for actuator health endpoint tests.
 * Provides MockMvc setup and test profile configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ActuatorHealthTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected static final String HEALTH_ENDPOINT = "/actuator/health";
    protected static final String LIVENESS_ENDPOINT = "/actuator/health/liveness";
    protected static final String READINESS_ENDPOINT = "/actuator/health/readiness";
}
