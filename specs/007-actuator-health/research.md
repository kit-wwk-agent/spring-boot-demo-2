# Research: Actuator Health Endpoint

**Feature**: 007-actuator-health
**Date**: 2026-03-08

## Executive Summary

Spring Boot Actuator health endpoint functionality is already partially implemented in the project. This research documents the current state, identifies gaps against the specification requirements, and recommends implementation approach.

## Current State Analysis

### Existing Actuator Configuration

The project already has Spring Boot Actuator configured in `application.yml`:

| Configuration | Current Value | Spec Requirement | Status |
|--------------|---------------|------------------|--------|
| Endpoint path | `/actuator/health` | `/actuator/health` | COMPLETE |
| HTTP 200 on healthy | Default Spring behavior | Required | COMPLETE |
| HTTP 503 on unhealthy | Default Spring behavior | Required | COMPLETE |
| Database health check | Included in readiness group | Required (FR-004) | COMPLETE |
| Disk space check | Enabled, 10MB threshold | Required (FR-005) | COMPLETE |
| No authentication | No Spring Security configured | Required (FR-007) | COMPLETE |
| JSON response | Default Spring behavior | Required (FR-008) | COMPLETE |
| Response time < 500ms | Cache TTL 1000ms | Required (FR-006) | NEEDS VERIFICATION |

### Gap Analysis

1. **Test Coverage**: No existing tests specifically verify all acceptance criteria
2. **Response Time Verification**: Need to validate < 500ms requirement under test
3. **Show Details Configuration**: Currently `when_authorized` in prod, but no auth exists - effectively hides details; need to decide if `{"status": "UP"}` format is acceptable or if component details should be shown

## Research Findings

### Decision 1: Health Response Format

**Decision**: Accept current Spring Boot Actuator default format
**Rationale**: The spec requires `{"status": "UP"}` or `{"status": "DOWN"}` format, which matches Spring Boot Actuator's default response format exactly
**Alternatives Considered**:
- Custom health endpoint controller: Rejected (unnecessary complexity, Spring handles this)
- Custom response serializer: Rejected (default format already compliant)

### Decision 2: Database Health Indicator

**Decision**: Use built-in `DataSourceHealthIndicator` (auto-configured)
**Rationale**: Spring Boot automatically includes database health check when `spring-boot-starter-data-jpa` is present; already configured in readiness group
**Alternatives Considered**:
- Custom DB health indicator: Rejected (built-in handles connection testing properly)
- Direct JDBC ping: Rejected (DataSourceHealthIndicator already does this)

### Decision 3: Disk Space Health Indicator

**Decision**: Use built-in `DiskSpaceHealthIndicator` with existing 10MB threshold
**Rationale**: Already configured in `application.yml` with `threshold: 10485760` (10MB); spec accepts "standard disk space thresholds"
**Alternatives Considered**:
- Custom disk space checker: Rejected (built-in is sufficient)
- Percentage-based threshold: Rejected (absolute threshold more predictable)

### Decision 4: Response Time Constraint (< 500ms)

**Decision**: Rely on existing 1000ms cache TTL plus add timeout test verification
**Rationale**: Health checks are lightweight (JDBC ping, disk stat call); caching prevents repeated expensive operations
**Alternatives Considered**:
- Async health checks: Rejected (adds complexity, not needed for current checks)
- Circuit breaker on health checks: Rejected (overkill for simple checks)

### Decision 5: Status Aggregation

**Decision**: Use Spring Boot default aggregation (worst status wins)
**Rationale**: Spring Boot's `StatusAggregator` returns the worst status (DOWN > UNKNOWN > UP), matching spec requirement: "overall status should reflect the worst individual status"
**Alternatives Considered**:
- Custom status aggregator: Rejected (default behavior matches requirement)

### Decision 6: Show Details Configuration

**Decision**: Keep `show-details: when_authorized` for prod, `always` for dev
**Rationale**: Spec requires `{"status": "UP"}` format which is always returned; component details are hidden in prod for security (prevents information disclosure about database/infrastructure)
**Alternatives Considered**:
- `show-details: always` everywhere: Rejected (security concern in production)
- `show-details: never`: Not needed (current config sufficient)

## Best Practices Applied

### Spring Boot Actuator Best Practices
- Use probe groups (liveness/readiness) for Kubernetes compatibility
- Enable caching to prevent thundering herd on health checks
- Separate liveness (app alive) from readiness (app can serve traffic)
- Hide implementation details in production environments

### Health Check Best Practices
- Keep health checks lightweight and fast
- Include only critical dependencies in health aggregation
- Use appropriate timeout configuration for dependency checks
- Log health status changes for observability

## Implementation Recommendations

1. **No code changes required to core functionality** - Actuator is properly configured
2. **Add comprehensive test coverage** verifying:
   - HTTP 200 with `{"status": "UP"}` when healthy
   - HTTP 503 with `{"status": "DOWN"}` when database unavailable
   - Response time < 500ms
   - No authentication required
   - JSON content type returned
3. **Verify disk space indicator** triggers properly (may require mock or config test)
4. **Document** the health endpoint behavior for operations teams

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-boot-starter-actuator | 3.4.3 | Health endpoint infrastructure |
| spring-boot-starter-data-jpa | 3.4.3 | Database health indicator |
| H2 Database (test) | (managed) | Test database for health check verification |

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Database health check timeout | Low | Medium | HikariCP timeout already set to 250ms |
| Disk space false positive | Low | Low | 10MB threshold is conservative |
| Cache staleness | Low | Low | 1000ms TTL balances freshness and performance |
