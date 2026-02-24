# Quickstart: Application Health Endpoint

**Feature**: 001-actuator-health
**Date**: 2026-02-24

## Prerequisites

- Java 21 (LTS)
- Gradle 8.x (wrapper included)
- Docker (optional, for PostgreSQL)

## Local Development

### 1. Start the Application

```bash
# Run with H2 embedded database (default)
./gradlew bootRun

# Or run with PostgreSQL (requires Docker)
docker run -d --name postgres -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:15
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 2. Test Health Endpoints

```bash
# Basic health check
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Kubernetes liveness probe
curl http://localhost:8080/actuator/health/liveness
# Expected: {"status":"UP"}

# Kubernetes readiness probe
curl http://localhost:8080/actuator/health/readiness
# Expected: {"status":"UP"}
```

### 3. Test Failure Scenarios

**Simulate database failure:**
```bash
# Stop PostgreSQL
docker stop postgres

# Check health
curl -w "\nHTTP Status: %{http_code}\n" http://localhost:8080/actuator/health
# Expected: {"status":"DOWN"}, HTTP Status: 503

# Restore
docker start postgres
```

**Check disk space threshold:**
```bash
# View current disk space status
curl http://localhost:8080/actuator/health | jq '.components.diskSpace'
```

## Configuration

### application.yml (Key Settings)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized  # Detailed status for authenticated users
      probes:
        enabled: true                # Enable liveness/readiness endpoints
      cache:
        time-to-live: 1000ms        # Cache health results for 1 second
  health:
    diskspace:
      threshold: 10485760           # 10MB minimum free space
```

### Connection Pool Settings (Performance)

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 250       # Max 250ms to acquire connection
      validation-timeout: 250       # Max 250ms for validation query
```

## Kubernetes Deployment

### Pod Spec with Probes

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: demo-app
spec:
  containers:
    - name: app
      image: demo:latest
      ports:
        - containerPort: 8080
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 30
        periodSeconds: 10
        timeoutSeconds: 1
        failureThreshold: 3
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
        timeoutSeconds: 1
        failureThreshold: 3
```

## Testing

### Run Tests

```bash
# All tests
./gradlew test

# Health endpoint tests only
./gradlew test --tests "*HealthEndpoint*"
```

### Key Test Scenarios

| Test | Description | Expected |
|------|-------------|----------|
| `testHealthyEndpoint` | All dependencies UP | 200, `{"status":"UP"}` |
| `testDatabaseDown` | DB unreachable | 503, `{"status":"DOWN"}` |
| `testDiskSpaceLow` | Free space < threshold | 503, `{"status":"DOWN"}` |
| `testLivenessProbe` | Liveness endpoint | 200 (ignores DB status) |
| `testReadinessProbe` | Readiness endpoint | Reflects DB/disk status |
| `testResponseTime` | Performance SLA | < 500ms |

## Troubleshooting

### Health endpoint returns 404

Ensure `spring-boot-starter-actuator` is in dependencies and endpoint is exposed:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
```

### Health check times out

Check database connectivity and connection pool settings:

```bash
# Test database directly
docker exec -it postgres psql -U postgres -c "SELECT 1"
```

### Liveness probe causes pod restarts

Verify liveness probe does NOT include database check:

```yaml
management:
  endpoint:
    health:
      group:
        liveness:
          include: livenessState  # Only livenessState, NOT db
```

## API Reference

See [contracts/health-api.yaml](./contracts/health-api.yaml) for full OpenAPI specification.

| Endpoint | Purpose | Checks Dependencies |
|----------|---------|---------------------|
| `/actuator/health` | Full health status | Yes |
| `/actuator/health/liveness` | Kubernetes liveness | No |
| `/actuator/health/readiness` | Kubernetes readiness | Yes |
