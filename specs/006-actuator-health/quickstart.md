# Quickstart: Actuator Health Endpoint

**Feature Branch**: `006-actuator-health`
**Date**: 2026-02-27

## Prerequisites

- Java 21 (LTS)
- Gradle 8.x (wrapper included)

## Quick Start

### 1. Start the Application

```bash
./gradlew bootRun
```

### 2. Test Health Endpoints

**Basic health check:**
```bash
curl http://localhost:8080/actuator/health
```

Expected response (when healthy):
```json
{"status":"UP"}
```

**Detailed health check (in dev/test mode):**
```bash
curl http://localhost:8080/actuator/health | jq
```

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 234567890123,
        "threshold": 10485760,
        "path": "/",
        "exists": true
      }
    }
  }
}
```

### 3. Kubernetes Probe Endpoints

**Liveness probe:**
```bash
curl http://localhost:8080/actuator/health/liveness
```

**Readiness probe:**
```bash
curl http://localhost:8080/actuator/health/readiness
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run only health endpoint integration tests
./gradlew test --tests '*HealthEndpointIntegrationTest'
```

## Configuration

### application.yml (already configured)

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
      threshold: 10485760
      path: /
```

### Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `management.endpoint.health.show-details` | `when_authorized` | Show component details |
| `management.endpoint.health.probes.enabled` | `true` | Enable liveness/readiness endpoints |
| `management.health.diskspace.threshold` | `10485760` (10MB) | Disk space warning threshold |
| `management.endpoint.health.cache.time-to-live` | `1000ms` | Cache health results |

## Kubernetes Deployment

### Example Pod Spec

```yaml
spec:
  containers:
    - name: demo
      image: demo:latest
      ports:
        - containerPort: 8080
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 30
        periodSeconds: 10
        failureThreshold: 3
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 5
        periodSeconds: 5
        failureThreshold: 3
```

## Troubleshooting

### Health endpoint returns 503

Check which component is DOWN:
```bash
curl -v http://localhost:8080/actuator/health
```

Common causes:
- Database unreachable: Check database connection settings
- Disk space low: Free up disk space or adjust threshold

### Details not showing

If you only see `{"status":"UP"}` without components:
- Check `show-details` configuration
- In production, details are hidden for unauthenticated requests

### Slow health checks

If health checks take > 500ms:
- Check database connection pool settings
- Verify HikariCP connection timeout
- Review disk space path accessibility
