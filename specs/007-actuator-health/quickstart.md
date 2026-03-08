# Quickstart: Actuator Health Endpoint

**Feature**: 007-actuator-health
**Date**: 2026-03-08

## Overview

This feature provides a health check endpoint at `/actuator/health` that reports application health status including database connectivity and disk space checks.

## Prerequisites

- Java 21 installed
- Gradle wrapper available (`./gradlew`)

## Quick Start

### 1. Start the Application

```bash
# Start with dev profile (H2 in-memory database)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 2. Test the Health Endpoint

```bash
# Basic health check
curl http://localhost:8080/actuator/health
```

**Expected Response (healthy)**:
```json
{"status":"UP"}
```

### 3. View Detailed Health (Dev Mode)

In dev profile, full details are shown:

```bash
curl http://localhost:8080/actuator/health | jq
```

**Expected Response**:
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
    }
  }
}
```

## Health Probe Endpoints

| Endpoint | Purpose | Usage |
|----------|---------|-------|
| `/actuator/health` | Overall health | Load balancers |
| `/actuator/health/liveness` | Is app alive? | Kubernetes liveness probe |
| `/actuator/health/readiness` | Can app serve traffic? | Kubernetes readiness probe |

## Configuration

Health endpoint behavior is configured in `application.yml`:

| Setting | Dev | Prod |
|---------|-----|------|
| Show details | Always | When authorized |
| Disk space threshold | 10MB | 10MB |
| Cache TTL | 1000ms | 1000ms |

## Running Tests

```bash
# Run all tests
./gradlew test

# Run only health endpoint tests
./gradlew test --tests '*ActuatorHealth*'
```

## Troubleshooting

### Health returns DOWN

1. **Database issue**: Check database connectivity
   ```bash
   curl http://localhost:8080/actuator/health/readiness
   ```

2. **Disk space issue**: Check available disk space
   ```bash
   df -h /
   ```

### No response from health endpoint

1. Verify application is running
2. Check port 8080 is not blocked
3. Verify actuator endpoints are exposed:
   ```bash
   curl http://localhost:8080/actuator
   ```

## Next Steps

- See [Contract Documentation](./contracts/health-endpoint.md) for full API details
- See [Data Model](./data-model.md) for response schema details
- See [Research](./research.md) for design decisions
