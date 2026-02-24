# Research: Application Health Endpoint

**Date**: 2026-02-24
**Feature**: 001-actuator-health
**Status**: Complete

## Research Questions Addressed

### 1. Spring Boot Version Selection

**Decision**: Spring Boot 4.0.x (latest stable: 4.0.3)

**Rationale**: Spring Boot 4.0 is the latest stable release (Feb 2026), built on Spring Framework 6.2+ with enhanced Actuator features. Provides LTS support and modern Java 21 compatibility.

**Alternatives Considered**:
- Spring Boot 3.5.x: Still maintained but approaching EOL
- Spring Boot 3.4.x: Legacy, missing latest Actuator improvements

### 2. Built-in Health Indicators

**Decision**: Use auto-configured `DataSourceHealthIndicator` and `DiskSpaceHealthIndicator`

**Rationale**: Spring Boot Actuator provides these indicators out-of-the-box when dependencies are present. No custom implementation needed for FR-004 and FR-005.

| Indicator | Auto-Configuration Trigger | What It Checks |
|-----------|---------------------------|----------------|
| `DataSourceHealthIndicator` | DataSource bean present | Database connectivity via validation query |
| `DiskSpaceHealthIndicator` | Always active | Available disk space against configurable threshold |
| `LivenessStateHealthIndicator` | Kubernetes environment | Application liveness state |
| `ReadinessStateHealthIndicator` | Kubernetes environment | Application readiness state |

**Alternatives Considered**:
- Custom health indicators: Unnecessary complexity for standard checks
- Third-party libraries: Spring Boot native solution is preferred

### 3. Health Status HTTP Mapping

**Decision**: Use default Spring Boot mappings

**Rationale**: Default mappings satisfy FR-002 and FR-003 requirements exactly.

| Health Status | HTTP Code | Use Case |
|---------------|-----------|----------|
| `UP` | 200 OK | All dependencies healthy (FR-002) |
| `DOWN` | 503 Service Unavailable | Any dependency unhealthy (FR-003) |
| `OUT_OF_SERVICE` | 503 Service Unavailable | Maintenance mode |

**Alternatives Considered**:
- Custom HTTP mappings: Not needed, defaults match requirements

### 4. Kubernetes Probe Strategy

**Decision**: Separate liveness and readiness probe groups

**Rationale**: Best practice per Spring documentation. Liveness should NOT check external dependencies (avoids restart loops). Readiness SHOULD check critical dependencies (removes unhealthy pods from load balancer).

**Configuration Approach**:
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

**Alternatives Considered**:
- Single health endpoint for both probes: Risk of restart loops when DB is temporarily unavailable

### 5. Performance Optimization (500ms SLA)

**Decision**: Connection pool timeouts + health result caching

**Rationale**: FR-008 requires 500ms response time. Database connections are the slowest component.

**Configuration Approach**:
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

**Alternatives Considered**:
- Async health indicators: Adds complexity; timeout configuration sufficient
- No caching: Risk of timeout under load

### 6. Security Configuration

**Decision**: Permit unauthenticated access to health endpoint, hide details

**Rationale**: FR-006 requires unauthenticated access for orchestration tools. However, detailed component status (FR-007) should only be shown when appropriate.

**Configuration Approach**:
```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
      show-components: when_authorized
```

**Alternatives Considered**:
- `show-details: always`: Exposes internal architecture to public
- `show-details: never`: Cannot satisfy FR-007

### 7. Database Choice

**Decision**: H2 (embedded) for development, PostgreSQL-ready for production

**Rationale**: Allows local development without external dependencies while being production-ready. DataSource health indicator works identically with both.

**Alternatives Considered**:
- PostgreSQL only: Complicates local development
- MySQL: PostgreSQL is more common in Kubernetes deployments

## Dependency Decisions

### Required Dependencies

| Dependency | Purpose | Version Strategy |
|------------|---------|-----------------|
| `spring-boot-starter-actuator` | Health endpoint, metrics | Spring Boot managed |
| `spring-boot-starter-data-jpa` | Database connectivity, DataSource auto-config | Spring Boot managed |
| `spring-boot-starter-web` | HTTP endpoint exposure | Spring Boot managed |
| `h2` | Development database | Runtime only, Spring Boot managed |
| `postgresql` | Production database driver | Runtime only, Spring Boot managed |

### Test Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-test` | JUnit 5, MockMvc, assertions |

## Configuration Summary

### application.yml (Production)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
      show-components: when_authorized
      probes:
        enabled: true
      cache:
        time-to-live: 1000ms
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db, diskSpace
  health:
    diskspace:
      enabled: true
      threshold: 10485760  # 10MB
      path: /

spring:
  datasource:
    hikari:
      connection-timeout: 250
      validation-timeout: 250
```

## Requirement Coverage

| Requirement | Solution |
|-------------|----------|
| FR-001 | `spring-boot-starter-actuator` auto-exposes `/actuator/health` |
| FR-002 | Default behavior: UP → 200 with `{"status": "UP"}` |
| FR-003 | Default behavior: DOWN → 503 with `{"status": "DOWN"}` |
| FR-004 | `DataSourceHealthIndicator` auto-configured with JPA |
| FR-005 | `DiskSpaceHealthIndicator` always active |
| FR-006 | Endpoint exposed without authentication by default |
| FR-007 | `show-details: when_authorized` enables detailed status |
| FR-008 | HikariCP timeouts (250ms) + health caching (1s) |
