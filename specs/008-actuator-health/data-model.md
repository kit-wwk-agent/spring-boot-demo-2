# Data Model: Actuator Health Endpoint

**Feature**: 008-actuator-health | **Date**: 2026-03-11

## Overview

This feature uses Spring Boot Actuator's built-in health model. No database entities are created - the data model describes the response structure returned by the health endpoint.

---

## Entities

### HealthStatus

Represents the overall application health state.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| status | String | Aggregate health status | Required; one of: UP, DOWN, OUT_OF_SERVICE, UNKNOWN |
| components | Map<String, HealthComponent> | Individual health indicators | Optional; shown based on `show-components` setting |

**Status Values**:
- `UP`: All health indicators report healthy
- `DOWN`: At least one critical health indicator failed
- `OUT_OF_SERVICE`: Application is refusing traffic (manual or automatic)
- `UNKNOWN`: Health status cannot be determined

**State Transitions**:
```
UNKNOWN → UP (startup complete, all indicators healthy)
UP → DOWN (critical indicator fails)
DOWN → UP (all indicators recover)
UP → OUT_OF_SERVICE (readiness state changed)
OUT_OF_SERVICE → UP (readiness restored)
```

---

### HealthComponent

Represents an individual health indicator's status.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| status | String | Component health status | Required; same values as HealthStatus |
| details | Map<String, Object> | Component-specific details | Optional; shown based on `show-details` setting |

---

### Database Health Details

Details returned by the `db` health indicator.

| Field | Type | Description |
|-------|------|-------------|
| database | String | Database product name (e.g., "H2", "PostgreSQL") |
| validationQuery | String | Query used to validate connection (if configured) |

---

### Disk Space Health Details

Details returned by the `diskSpace` health indicator.

| Field | Type | Description |
|-------|------|-------------|
| total | Long | Total disk space in bytes |
| free | Long | Free disk space in bytes |
| threshold | Long | Configured threshold in bytes (10485760 = 10MB) |
| path | String | Filesystem path being monitored |
| exists | Boolean | Whether the path exists |

---

## Response Examples

### Healthy System (Unauthenticated)

```json
{
  "status": "UP"
}
```

### Healthy System (Authenticated / Test Profile)

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

### Unhealthy System (Database Down)

```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

## Health Groups

### Liveness Group (`/actuator/health/liveness`)

| Indicator | Purpose |
|-----------|---------|
| livenessState | Reports if application is alive |

### Readiness Group (`/actuator/health/readiness`)

| Indicator | Purpose |
|-----------|---------|
| readinessState | Reports if application accepts traffic |
| db | Database connectivity status |
| diskSpace | Disk space availability status |

---

## Relationships

```
HealthStatus (1) ──────────── (*) HealthComponent
                contains

HealthComponent ──────────── (0..1) Details
              may have
```

---

## Notes

- All entities are read-only response DTOs - no persistence layer
- Spring Boot Actuator provides these models via `org.springframework.boot.actuate.health.*`
- No custom entity classes needed for this feature
