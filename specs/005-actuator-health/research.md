# Research: Actuator Health Endpoint

**Feature**: 005-actuator-health
**Date**: 2026-02-26

## Research Summary

This document captures research findings for implementing a production-ready health check endpoint using Spring Boot Actuator.

---

## 1. Unauthenticated Health Endpoint Access

### Decision
Allow unauthenticated access to `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` endpoints while keeping other actuator endpoints secured.

### Rationale
- Kubernetes probes and load balancers require unauthenticated access to health endpoints
- This is the standard industry practice for container orchestration
- No Spring Security is currently configured in the project, so health endpoints are already accessible
- If security is added later, explicit permit rules will be needed

### Alternatives Considered
- **Authenticated health endpoints**: Rejected because it complicates Kubernetes probe configuration and requires credential management in infrastructure
- **Separate health port**: Rejected because it adds operational complexity without significant benefit

### Implementation
Since no Spring Security dependency exists, endpoints are accessible by default. The current configuration uses `show-details: when_authorized` which shows basic status to all but hides component details from unauthenticated users.

---

## 2. Health Check Timeout Configuration

### Decision
Configure health indicator timeouts to ensure total response time stays within 500ms threshold:
- Database health check: 250ms (already configured via HikariCP)
- Disk space check: No explicit timeout needed (file system operation)
- Response caching: 1 second (already configured)

### Rationale
- FR-006 requires 500ms response time under normal conditions
- Current HikariCP configuration has `connection-timeout: 250` and `validation-timeout: 250`
- Response caching at 1000ms prevents rapid repeated health checks
- Spring Boot doesn't have global timeout; individual indicators must handle timeouts

### Alternatives Considered
- **Custom health indicators with ExecutorService timeouts**: Adds complexity without benefit since default indicators are sufficient
- **No caching**: Rejected because it increases load and response time variability

### Implementation
Existing application.yml configuration is appropriate:
```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 250
      validation-timeout: 250

management:
  endpoint:
    health:
      cache:
        time-to-live: 1000ms
```

---

## 3. Liveness vs Readiness Probe Separation

### Decision
Configure separate health indicator groups:
- **Liveness** (`/actuator/health/liveness`): Only `livenessState` - checks if JVM is alive
- **Readiness** (`/actuator/health/readiness`): `readinessState`, `db`, `diskSpace` - checks if ready for traffic

### Rationale
- Liveness probe failure triggers container restart; should NOT include external dependencies
- Readiness probe failure removes pod from load balancer; appropriate for dependency checks
- Prevents cascading restarts when database is temporarily unavailable
- Matches Kubernetes best practices and Spring Boot 2.3+ design

### Alternatives Considered
- **Single health endpoint for both probes**: Rejected because database unavailability would cause container restarts
- **Custom health groups with additional indicators**: Not needed for current requirements (only db and disk space)

### Implementation
Existing application.yml is correctly configured:
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

---

## 4. Database Health Indicator

### Decision
Use Spring Boot's built-in `DataSourceHealthIndicator` with current HikariCP timeout settings.

### Rationale
- Auto-registered when DataSource bean exists
- Performs validation query to check connectivity
- Respects HikariCP connection timeout settings
- No custom indicator needed

### Alternatives Considered
- **Custom database health indicator with explicit query**: Adds maintenance overhead without benefit
- **Disable database health check**: Does not meet FR-004 requirement

### Implementation
No additional configuration needed. Spring Boot auto-detects:
- H2 DataSource in dev/test profiles
- PostgreSQL DataSource in prod profile

Health check performs:
1. Get connection from pool (250ms timeout)
2. Validate connection (250ms timeout)
3. Return UP/DOWN based on result

---

## 5. Disk Space Health Indicator

### Decision
Use Spring Boot's built-in `DiskSpaceHealthIndicator` with 10MB threshold monitoring the root path.

### Rationale
- Already configured in application.yml
- 10MB threshold (10485760 bytes) provides early warning
- Monitoring root path (`/`) covers the primary disk
- Standard Spring Boot configuration

### Alternatives Considered
- **Higher threshold (100MB+)**: May cause false positives in constrained environments
- **Monitor /tmp path**: Not necessary unless application heavily uses temp files
- **Custom disk space indicator**: Unnecessary complexity

### Implementation
Current application.yml configuration:
```yaml
management:
  health:
    diskspace:
      enabled: true
      threshold: 10485760  # 10MB in bytes
      path: /
```

---

## 6. Kubernetes Probe Configuration (Recommendations)

### Decision
Document recommended Kubernetes probe settings for deployment manifests.

### Recommended Configuration
```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  failureThreshold: 30
  periodSeconds: 10
  timeoutSeconds: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 8
  periodSeconds: 4
  timeoutSeconds: 1
  failureThreshold: 3

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 15
  timeoutSeconds: 3
  failureThreshold: 5
```

### Rationale
- Startup probe allows up to 300 seconds for slow Java startup
- Readiness probe has short intervals for quick traffic routing decisions
- Liveness probe has longer intervals and higher threshold to prevent false restarts
- Timeout values accommodate production load conditions

---

## 7. Response Format

### Decision
Use Spring Boot Actuator's default JSON response format with component details visible based on authorization.

### Response Examples

**Basic health (unauthenticated)**:
```json
{
  "status": "UP"
}
```

**Detailed health (dev profile or authorized)**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 250000000000,
        "threshold": 10485760,
        "path": "/.",
        "exists": true
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

**DOWN status (HTTP 503)**:
```json
{
  "status": "DOWN"
}
```

### Rationale
- Matches FR-002, FR-003, FR-008 requirements
- HTTP 200 for UP, HTTP 503 for DOWN
- Content-Type: application/json
- Component details available in dev/test profiles

---

## Configuration Summary

### Current Configuration Status

| Requirement | Status | Notes |
|------------|--------|-------|
| Health endpoint at /actuator/health | DONE | Already configured |
| Database health check | DONE | Auto-registered with DataSource |
| Disk space health check | DONE | Configured with 10MB threshold |
| Liveness/readiness separation | DONE | Probe groups configured |
| Unauthenticated access | DONE | No Spring Security present |
| 500ms response threshold | NEEDS VALIDATION | HikariCP timeouts configured |
| JSON response format | DONE | Default actuator behavior |

### Required Work

1. **Production profile configuration**: Create `application-prod.yml` with appropriate settings
2. **Integration tests**: Verify health endpoint behavior for all scenarios
3. **Documentation**: Create quickstart guide for Kubernetes deployment

---

## Sources

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Liveness and Readiness Probes with Spring Boot](https://spring.io/blog/2020/03/25/liveness-and-readiness-probes-with-spring-boot/)
- [Baeldung: Liveness and Readiness Probes](https://www.baeldung.com/spring-liveness-readiness-probes)
- [Baeldung: Health Indicators in Spring Boot](https://www.baeldung.com/spring-boot-health-indicators)
- [Kubernetes Configure Liveness, Readiness, and Startup Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
