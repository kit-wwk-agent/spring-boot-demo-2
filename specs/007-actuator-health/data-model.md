# Data Model: Actuator Health Endpoint

**Feature**: 007-actuator-health
**Date**: 2026-03-08

## Overview

This feature uses Spring Boot Actuator's built-in health infrastructure. No new persistent entities are required. This document describes the conceptual data model for health status representation.

## Entities

### Health Status (Conceptual - Spring Boot Managed)

The health status is managed by Spring Boot Actuator, not persisted to a database.

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| status | String | Overall health status | Enum: UP, DOWN, UNKNOWN, OUT_OF_SERVICE |
| components | Map<String, ComponentHealth> | Individual health indicators | Optional (based on show-details config) |

**State Transitions**:
```
UP <---> DOWN
 ^        ^
 |        |
 v        v
UNKNOWN <--> OUT_OF_SERVICE
```

**Aggregation Rule**: Overall status = worst status among all components
- Priority: DOWN > OUT_OF_SERVICE > UNKNOWN > UP

### Component Health (Conceptual)

| Field | Type | Description |
|-------|------|-------------|
| status | String | Component health status (UP/DOWN) |
| details | Map<String, Object> | Component-specific details (optional) |

### Health Indicators Included

| Indicator | Source | Check Description |
|-----------|--------|-------------------|
| db | DataSourceHealthIndicator | Executes validation query against database |
| diskSpace | DiskSpaceHealthIndicator | Checks available disk space against threshold |
| livenessState | LivenessStateHealthIndicator | Application liveness for Kubernetes |
| readinessState | ReadinessStateHealthIndicator | Application readiness for Kubernetes |

## Response Schemas

### Basic Health Response (show-details: never or when_authorized without auth)

```json
{
  "status": "UP"
}
```

### Detailed Health Response (show-details: always or when_authorized with auth)

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
        "free": 123456789012,
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

### Unhealthy Response

```json
{
  "status": "DOWN"
}
```

Or with details:

```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "org.springframework.jdbc.CannotGetJdbcConnectionException: ..."
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

## Validation Rules

| Rule | Description | Error Condition |
|------|-------------|-----------------|
| Database connectivity | JDBC connection must be valid | Connection fails or times out (250ms) |
| Disk space | Free space must exceed threshold | Free space < 10MB (10485760 bytes) |

## Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `management.endpoint.health.show-details` | Enum | when_authorized | When to show component details |
| `management.endpoint.health.show-components` | Enum | when_authorized | When to show component list |
| `management.health.diskspace.threshold` | Long | 10485760 | Minimum free disk space in bytes |
| `management.health.diskspace.path` | String | / | Path to check for disk space |
| `management.endpoint.health.cache.time-to-live` | Duration | 1000ms | Health check cache duration |

## No Persistent Storage Required

This feature does not introduce new database entities. All health status data is:
- Computed on-demand (with caching)
- Ephemeral and stateless
- Based on runtime checks of existing infrastructure
