# Research: Actuator Health Endpoint

**Feature**: 008-actuator-health | **Date**: 2026-03-11

## Overview

This document captures research findings and design decisions for implementing the health endpoint. No NEEDS CLARIFICATION items were identified in the Technical Context - Spring Boot Actuator provides built-in support for all requirements.

---

## Decision 1: Health Endpoint Framework

**Decision**: Use Spring Boot Actuator's built-in health endpoint

**Rationale**:
- Actuator is already a dependency in the project (`spring-boot-starter-actuator`)
- Provides `/actuator/health` endpoint out of the box
- Built-in health indicators for database (DataSourceHealthIndicator) and disk space (DiskSpaceHealthIndicator)
- Supports Kubernetes probes via health groups (liveness/readiness)
- Industry standard for Spring Boot health monitoring

**Alternatives Considered**:
- Custom health controller: Rejected - would duplicate Actuator functionality without benefits
- Third-party health libraries: Rejected - Actuator is the de facto standard for Spring Boot

---

## Decision 2: Health Probe Strategy (Liveness vs Readiness)

**Decision**: Separate liveness and readiness probes with appropriate health indicators

**Configuration**:
- **Liveness probe** (`/actuator/health/liveness`): Only includes `livenessState` - checks if app is alive and should not be restarted
- **Readiness probe** (`/actuator/health/readiness`): Includes `readinessState`, `db`, `diskSpace` - checks if app can accept traffic

**Rationale**:
- Liveness probes should be lightweight and not include external dependencies (database failures shouldn't trigger pod restarts)
- Readiness probes should verify the app can serve traffic (database connectivity matters)
- This pattern follows Kubernetes best practices for probe design

**Alternatives Considered**:
- Single health endpoint for both: Rejected - doesn't align with Kubernetes probe semantics
- Include database in liveness: Rejected - transient DB issues would cause unnecessary pod restarts

---

## Decision 3: Security Configuration

**Decision**: Allow unauthenticated access to health endpoints

**Configuration**:
```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
      show-components: when_authorized
```

**Rationale**:
- Kubernetes probes and load balancers cannot provide authentication tokens
- Basic status (UP/DOWN) is safe to expose publicly
- Detailed component information hidden from unauthenticated users (`when_authorized`)
- No Spring Security configured - endpoints are publicly accessible by default

**Alternatives Considered**:
- Full authentication: Rejected - breaks orchestration tool integration
- Show all details publicly: Rejected - exposes internal system information

---

## Decision 4: Health Indicator Configuration

**Decision**: Use Spring Boot's auto-configured health indicators with custom thresholds

**Database Health**:
- Uses `DataSourceHealthIndicator` (auto-configured with JPA)
- Validates connectivity with query timeout
- Reports UP when connection succeeds, DOWN when fails

**Disk Space Health**:
- Uses `DiskSpaceHealthIndicator`
- Threshold: 10MB (`threshold: 10485760` bytes)
- Path: root filesystem (`/`)
- Reports DOWN when free space falls below threshold

**Rationale**:
- Auto-configuration reduces boilerplate
- 10MB threshold is reasonable minimum for logs/temp files
- Connection timeout (250ms in HikariCP) ensures fast failure detection

**Alternatives Considered**:
- Custom health indicators: Not needed - built-in indicators meet all requirements
- Higher disk threshold: Could be adjusted per deployment environment

---

## Decision 5: Response Time Optimization

**Decision**: Configure caching and connection timeouts to meet 500ms SLA

**Configuration**:
```yaml
management:
  endpoint:
    health:
      cache:
        time-to-live: 1000ms
spring:
  datasource:
    hikari:
      connection-timeout: 250
      validation-timeout: 250
```

**Rationale**:
- 1-second cache prevents excessive database checks under high polling
- 250ms connection timeout ensures fast failure for DB checks
- Total response time well under 500ms requirement

**Alternatives Considered**:
- No caching: Rejected - could cause excessive load under high-frequency polling
- Longer cache TTL: Acceptable tradeoff for freshness vs. performance

---

## Implementation Notes

The feature implementation primarily involves YAML configuration changes:

1. **application.yml**: Core health endpoint configuration
2. **application-test.yml**: Test profile with `show-details: always` for assertion access

No custom Java code required - Spring Boot Actuator auto-configuration handles all requirements:
- Health endpoint exposure
- Database health indicator (via JPA autoconfiguration)
- Disk space health indicator (enabled by default)
- HTTP status code mapping (200 for UP, 503 for DOWN)
- JSON response formatting
