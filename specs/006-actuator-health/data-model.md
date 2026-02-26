# Data Model: Actuator Health Endpoint

**Feature Branch**: `006-actuator-health`
**Date**: 2026-02-27

## Overview

The health endpoint data model is defined by Spring Boot Actuator's built-in types. No custom entities are required for this feature - the model is configuration-driven using standard Actuator response structures.

## Entity Definitions

### HealthResponse

The top-level response returned by the health endpoint.

| Field | Type | Description |
|-------|------|-------------|
| `status` | HealthStatus | Aggregated health status (UP, DOWN) |
| `components` | Map<String, HealthComponent> | Individual health indicators (optional, depends on config) |

**JSON Schema**:
```json
{
  "type": "object",
  "required": ["status"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN"]
    },
    "components": {
      "type": "object",
      "additionalProperties": {
        "$ref": "#/definitions/HealthComponent"
      }
    }
  }
}
```

### HealthStatus

Enumeration of possible health states.

| Value | HTTP Status | Description |
|-------|-------------|-------------|
| `UP` | 200 OK | All health indicators pass |
| `DOWN` | 503 Service Unavailable | One or more critical indicators fail |
| `OUT_OF_SERVICE` | 503 Service Unavailable | Manually marked out of service |
| `UNKNOWN` | 200 OK | Health status cannot be determined |

### HealthComponent

Individual health indicator status with optional details.

| Field | Type | Description |
|-------|------|-------------|
| `status` | HealthStatus | Status of this specific indicator |
| `details` | Map<String, Object> | Component-specific details (optional) |

## Built-in Health Indicators

### DataSourceHealthIndicator (db)

Automatically included when Spring Data JPA is configured.

| Detail Field | Type | Description |
|--------------|------|-------------|
| `database` | String | Database product name (e.g., "H2", "PostgreSQL") |
| `validationQuery` | String | Query used to validate connection (optional) |

**Example Response**:
```json
{
  "status": "UP",
  "details": {
    "database": "H2",
    "validationQuery": "isValid()"
  }
}
```

### DiskSpaceHealthIndicator (diskSpace)

Monitors available disk space against configured threshold.

| Detail Field | Type | Description |
|--------------|------|-------------|
| `total` | Long | Total disk space in bytes |
| `free` | Long | Free disk space in bytes |
| `threshold` | Long | Configured threshold in bytes |
| `path` | String | Path being monitored |
| `exists` | Boolean | Whether the path exists |

**Example Response**:
```json
{
  "status": "UP",
  "details": {
    "total": 499963174912,
    "free": 234567890123,
    "threshold": 10485760,
    "path": "/",
    "exists": true
  }
}
```

### LivenessStateHealthIndicator (livenessState)

Kubernetes liveness probe indicator.

| Detail Field | Type | Description |
|--------------|------|-------------|
| N/A | N/A | Returns only status, no details |

### ReadinessStateHealthIndicator (readinessState)

Kubernetes readiness probe indicator.

| Detail Field | Type | Description |
|--------------|------|-------------|
| N/A | N/A | Returns only status, no details |

## Validation Rules

| Rule | Indicator | Condition |
|------|-----------|-----------|
| Database Reachable | db | Connection can be established and validation query succeeds |
| Disk Space Available | diskSpace | Free space >= configured threshold (10MB default) |
| Application Started | livenessState | Application context has started |
| Application Ready | readinessState | Application is ready to accept traffic |

## State Transitions

### Aggregated Health Status

The overall health status follows this aggregation logic:

```
IF any indicator is DOWN → Overall status is DOWN
ELSE IF any indicator is OUT_OF_SERVICE → Overall status is OUT_OF_SERVICE
ELSE IF any indicator is UNKNOWN → Overall status is UNKNOWN
ELSE → Overall status is UP
```

### Health Groups

| Group | Indicators | Purpose |
|-------|------------|---------|
| `liveness` | livenessState | Kubernetes: Should container restart? |
| `readiness` | readinessState, db, diskSpace | Kubernetes: Should container receive traffic? |

## Configuration Impact

| Configuration | Effect on Response |
|---------------|-------------------|
| `show-details: never` | Only `status` field returned |
| `show-details: when_authorized` | Details shown only to authenticated users |
| `show-details: always` | Full details always included |
| `show-components: never` | `components` field omitted |
| `show-components: when_authorized` | Components shown only to authenticated users |
| `show-components: always` | All components always included |

## Related Entities

This feature does not introduce new database entities. It monitors the health of existing infrastructure:

- **DataSource**: Existing HikariCP connection pool to H2/PostgreSQL
- **File System**: Root path "/" for disk space monitoring
