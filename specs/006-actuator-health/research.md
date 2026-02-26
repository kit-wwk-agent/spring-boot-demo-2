# Research: Actuator Health Endpoint

**Feature Branch**: `006-actuator-health`
**Date**: 2026-02-27

## Executive Summary

Spring Boot Actuator provides production-ready health monitoring features out of the box. This feature leverages the built-in health infrastructure with configuration-only customization. No custom health indicators are required for the specified database connectivity and disk space checks.

## Research Items

### 1. Health Endpoint Implementation Approach

**Decision**: Use Spring Boot Actuator's built-in health endpoint configuration

**Rationale**:
- Spring Boot Actuator provides `/actuator/health` endpoint automatically when included as a dependency
- Built-in `DataSourceHealthIndicator` automatically checks database connectivity when JPA is configured
- Built-in `DiskSpaceHealthIndicator` monitors disk space with configurable thresholds
- No custom code required - pure configuration in `application.yml`

**Alternatives Considered**:
| Alternative | Why Rejected |
|-------------|--------------|
| Custom health endpoint controller | Reinvents existing actuator functionality, more code to maintain |
| Custom health indicators | Not needed - built-in indicators cover database and disk space |
| Third-party health library | Unnecessary dependency when Spring Boot provides this natively |

### 2. Health Response Format

**Decision**: Use Spring Boot Actuator's standard JSON response format

**Rationale**:
- Standard format is widely recognized by monitoring tools (Prometheus, Grafana, Kubernetes)
- Returns `{"status": "UP"}` or `{"status": "DOWN"}` as specified in requirements
- Configurable detail levels via `show-details` and `show-components` properties

**Response Structure**:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "H2" } },
    "diskSpace": { "status": "UP", "details": { "total": 499963174912, "free": 123456789, "threshold": 10485760 } }
  }
}
```

### 3. Kubernetes Probe Support

**Decision**: Enable Kubernetes probe endpoints (`/actuator/health/liveness`, `/actuator/health/readiness`)

**Rationale**:
- Spring Boot 2.3+ provides built-in support for Kubernetes probes
- Liveness probe determines if container should be restarted
- Readiness probe determines if container can receive traffic
- Health groups allow different indicators for each probe type

**Configuration Applied**:
```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db, diskSpace
```

### 4. Authentication for Health Endpoints

**Decision**: No authentication required for health endpoints

**Rationale**:
- Per FR-006: "System MUST allow unauthenticated access to the health endpoint"
- Kubernetes probes cannot easily authenticate
- Current project has no Spring Security configured
- If Spring Security is added later, explicit permit rules will be needed

**Security Notes**:
- `show-details: when_authorized` hides component details from unauthenticated users
- Production environments may want to expose only aggregated status publicly

### 5. Disk Space Threshold

**Decision**: Configure 10MB (10485760 bytes) threshold for disk space health indicator

**Rationale**:
- Reasonable default for alerting before complete disk exhaustion
- Lower than Spring Boot's default (10MB matches spec assumptions)
- Configurable per-environment if needed

**Configuration Applied**:
```yaml
management:
  health:
    diskspace:
      enabled: true
      threshold: 10485760
      path: /
```

### 6. Performance Considerations

**Decision**: Cache health check results for 1 second

**Rationale**:
- FR-007 requires < 500ms response time
- Caching prevents redundant database queries on frequent health checks
- 1 second TTL balances freshness with performance

**Configuration Applied**:
```yaml
management:
  endpoint:
    health:
      cache:
        time-to-live: 1000ms
```

### 7. Connection Timeout Configuration

**Decision**: Set aggressive connection timeouts for health checks

**Rationale**:
- Health checks should fail fast if database is unreachable
- 250ms connection timeout prevents health endpoint from blocking

**Configuration Applied**:
```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 250
      validation-timeout: 250
```

## Implementation Status

The health endpoint configuration is already implemented in the current codebase:

| Component | Status | Location |
|-----------|--------|----------|
| Actuator dependency | Complete | `build.gradle.kts` |
| Health endpoint config | Complete | `src/main/resources/application.yml` |
| Test configuration | Complete | `src/test/resources/application-test.yml` |
| Integration tests | Complete | `src/test/java/.../HealthEndpointIntegrationTest.java` |

## Remaining Work

1. Verify all integration tests pass
2. Document the health endpoints for API consumers
3. Consider adding custom health indicators for future requirements (if needed)

## References

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kubernetes Liveness and Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Spring Boot Health Indicators](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
