# Data Model: Application Health Endpoint

**Date**: 2026-02-24
**Feature**: 001-actuator-health

## Overview

This feature uses Spring Boot Actuator's built-in health model. No custom entities are persisted to the database. The models below describe the response structure exposed by the health endpoint.

## Entities

### HealthResponse

Represents the complete health check response returned by `/actuator/health`.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `status` | `Status` | Aggregate health status | `UP` |
| `components` | `Map<String, HealthComponent>` | Individual health indicator results | See below |

**JSON Example (Detailed)**:
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
        "path": "/",
        "exists": true
      }
    }
  }
}
```

**JSON Example (Minimal)**:
```json
{
  "status": "UP"
}
```

### Status (Enum)

Represents the health status of the application or a component.

| Value | Description | HTTP Code |
|-------|-------------|-----------|
| `UP` | Healthy, all checks passing | 200 |
| `DOWN` | Unhealthy, at least one check failing | 503 |
| `OUT_OF_SERVICE` | Temporarily unavailable | 503 |
| `UNKNOWN` | Status cannot be determined | 200 |

### HealthComponent

Represents the health status of an individual indicator.

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `status` | `Status` | Component health status | Yes |
| `details` | `Map<String, Object>` | Additional information about the component | No |

## Health Indicators

### DataSourceHealthIndicator (db)

**Trigger**: Automatically registered when a `DataSource` bean is present.

| Detail Field | Type | Description |
|--------------|------|-------------|
| `database` | `String` | Database product name (e.g., "H2", "PostgreSQL") |
| `validationQuery` | `String` | Method used to validate connection |

**Failure Conditions**:
- Database connection cannot be established
- Validation query times out (>250ms)
- Connection pool exhausted

### DiskSpaceHealthIndicator (diskSpace)

**Trigger**: Always registered by default.

| Detail Field | Type | Description |
|--------------|------|-------------|
| `total` | `Long` | Total disk space in bytes |
| `free` | `Long` | Available disk space in bytes |
| `threshold` | `Long` | Minimum required free space (default: 10MB) |
| `path` | `String` | Path being checked |
| `exists` | `Boolean` | Whether the path exists |

**Failure Conditions**:
- Free disk space < threshold
- Path does not exist

### LivenessStateHealthIndicator (livenessState)

**Trigger**: Enabled in Kubernetes or when `management.endpoint.health.probes.enabled=true`.

| Status | Meaning |
|--------|---------|
| `UP` | Application is running and can accept requests |
| `DOWN` | Application should be restarted |

### ReadinessStateHealthIndicator (readinessState)

**Trigger**: Enabled in Kubernetes or when `management.endpoint.health.probes.enabled=true`.

| Status | Meaning |
|--------|---------|
| `UP` | Application is ready to handle traffic |
| `OUT_OF_SERVICE` | Application should not receive traffic |

## Health Groups

### Liveness Group (`/actuator/health/liveness`)

For Kubernetes liveness probe. Does NOT check external dependencies.

**Included Indicators**:
- `livenessState`

**Rationale**: If liveness checked DB connectivity, a temporary database outage would cause all pods to restart, making recovery impossible.

### Readiness Group (`/actuator/health/readiness`)

For Kubernetes readiness probe. Checks all critical dependencies.

**Included Indicators**:
- `readinessState`
- `db`
- `diskSpace`

**Rationale**: Pods with failed dependencies should be removed from service but NOT restarted.

## State Transitions

```
Application Startup:
  └─> livenessState: DOWN
  └─> readinessState: OUT_OF_SERVICE
  └─> (initialization)
  └─> livenessState: UP
  └─> readinessState: UP (when all dependencies healthy)

Database Connection Loss:
  └─> db: DOWN
  └─> readinessState: OUT_OF_SERVICE
  └─> Kubernetes removes pod from service
  └─> (database recovers)
  └─> db: UP
  └─> readinessState: UP
  └─> Kubernetes adds pod back to service

Disk Space Exhaustion:
  └─> diskSpace: DOWN
  └─> readinessState: OUT_OF_SERVICE
  └─> (alert triggered, space freed)
  └─> diskSpace: UP
  └─> readinessState: UP
```

## Validation Rules

| Entity | Rule | Error Behavior |
|--------|------|----------------|
| HealthResponse | `status` must be non-null | N/A (Spring enforces) |
| DiskSpace | `threshold` must be >= 0 | Configuration error at startup |
| DiskSpace | `path` must exist | Returns `DOWN` status |
| DataSource | Connection must be valid | Returns `DOWN` status |

## Relationships

```
HealthResponse
├── 1:1 Status (aggregate)
└── 1:N HealthComponent
    └── 1:1 Status (component)
```

## Persistence

**This feature does not persist any data**. All health information is computed on-demand from:
- Database connection pool state
- Filesystem metadata
- Application lifecycle state
