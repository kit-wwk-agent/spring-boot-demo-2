# Data Model: Actuator Health Endpoint

**Feature**: 005-actuator-health
**Date**: 2026-02-26

## Overview

This feature uses Spring Boot Actuator's built-in health model. No custom entity persistence is required. The data model describes the response structures returned by the health endpoint.

---

## Entities

### 1. Health Status (Enum)

Represents the overall health state of the application or a component.

| Value | Description | HTTP Status |
|-------|-------------|-------------|
| `UP` | All monitored components are healthy | 200 OK |
| `DOWN` | One or more critical components are unhealthy | 503 Service Unavailable |
| `UNKNOWN` | Health status cannot be determined | 200 OK |
| `OUT_OF_SERVICE` | Component is explicitly out of service | 503 Service Unavailable |

**Spring Boot Class**: `org.springframework.boot.actuate.health.Status`

**Validation Rules**:
- Status aggregation: If ANY component is DOWN, overall status is DOWN
- Status hierarchy: DOWN > OUT_OF_SERVICE > UP > UNKNOWN

---

### 2. Component Health

Represents the health state of an individual system component.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | Status | Yes | Health status of the component |
| `details` | Map<String, Object> | No | Component-specific health details |

**Components in Scope**:

#### Database Component (`db`)
| Detail Field | Type | Description |
|--------------|------|-------------|
| `database` | String | Database product name (e.g., "H2", "PostgreSQL") |
| `validationQuery` | String | Query used to validate connection |

#### Disk Space Component (`diskSpace`)
| Detail Field | Type | Description |
|--------------|------|-------------|
| `total` | Long | Total disk space in bytes |
| `free` | Long | Free disk space in bytes |
| `threshold` | Long | Configured threshold in bytes |
| `path` | String | Monitored path |
| `exists` | Boolean | Whether the path exists |

#### Liveness State (`livenessState`)
| Detail Field | Type | Description |
|--------------|------|-------------|
| (none) | - | Returns only status (UP when JVM is alive) |

#### Readiness State (`readinessState`)
| Detail Field | Type | Description |
|--------------|------|-------------|
| (none) | - | Returns only status (UP when ready for traffic) |

---

### 3. Health Response

The structured response returned by the health endpoint.

| Field | Type | Required | Visibility | Description |
|-------|------|----------|------------|-------------|
| `status` | Status | Yes | Always | Overall aggregated health status |
| `components` | Map<String, ComponentHealth> | No | when_authorized | Individual component health details |

**Visibility Modes** (configured via `management.endpoint.health.show-details`):
- `never`: Only status field returned
- `when_authorized`: Components shown to authorized users only
- `always`: Full details shown to all users

---

## Relationships

```
┌─────────────────────┐
│   Health Response   │
├─────────────────────┤
│ status: Status      │──────────────┐
│ components: Map     │              │
└────────┬────────────┘              │
         │                           │
         │ 0..* contains             │
         ▼                           │
┌─────────────────────┐              │
│  Component Health   │              │
├─────────────────────┤              │
│ status: Status      │──────────────┤
│ details: Map        │              │
└─────────────────────┘              │
                                     │
                                     ▼
                          ┌─────────────────────┐
                          │       Status        │
                          ├─────────────────────┤
                          │ UP                  │
                          │ DOWN                │
                          │ UNKNOWN             │
                          │ OUT_OF_SERVICE      │
                          └─────────────────────┘
```

---

## State Transitions

### Application Startup Sequence

```
STARTING → (components initializing)
    │
    ▼
READINESS = DOWN (not ready for traffic)
LIVENESS = UP (JVM is alive)
    │
    ▼
(all components ready)
    │
    ▼
READINESS = UP (ready for traffic)
LIVENESS = UP
```

### Runtime Health State Changes

```
                    ┌───────────────────────────────┐
                    │                               │
                    ▼                               │
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────────┐
│   UP    │───►│  DOWN   │───►│   UP    │    │   DOWN      │
│         │    │         │    │         │    │ (sustained) │
└─────────┘    └─────────┘    └─────────┘    └─────────────┘
    │              │              │                │
    │              │              │                ▼
    │              │              │          Container
    │              │              │          Restart
    └──────────────┴──────────────┘

Transitions triggered by:
- Database unavailable → db status DOWN
- Disk space below threshold → diskSpace status DOWN
- Database reconnected → db status UP
- Disk space freed → diskSpace status UP
```

---

## Validation Rules

### Health Indicator Thresholds

| Component | Threshold | Action when Violated |
|-----------|-----------|---------------------|
| Database | Connection timeout 250ms | Status → DOWN |
| Database | Validation timeout 250ms | Status → DOWN |
| Disk Space | Free < 10MB | Status → DOWN |

### Response Time Constraints

| Constraint | Value | Source |
|------------|-------|--------|
| Max response time | 500ms | FR-006 |
| DB check timeout | 250ms | HikariCP config |
| Cache TTL | 1000ms | Actuator config |

---

## JSON Schema

### Basic Health Response

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["UP", "DOWN", "UNKNOWN", "OUT_OF_SERVICE"]
    }
  }
}
```

### Detailed Health Response

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["status"],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["UP", "DOWN", "UNKNOWN", "OUT_OF_SERVICE"]
    },
    "components": {
      "type": "object",
      "additionalProperties": {
        "type": "object",
        "required": ["status"],
        "properties": {
          "status": {
            "type": "string",
            "enum": ["UP", "DOWN", "UNKNOWN", "OUT_OF_SERVICE"]
          },
          "details": {
            "type": "object"
          }
        }
      }
    }
  }
}
```

---

## Notes

- No custom entities need to be created; Spring Boot Actuator provides all necessary classes
- Health data is not persisted; computed on-demand with caching
- Response structure is standardized by Spring Boot Actuator
- Additional health indicators can be added without schema changes
