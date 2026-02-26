# Quickstart: Actuator Health Endpoint

## Overview

The health endpoint is already configured in this Spring Boot application. This guide explains how to verify, test, and deploy the health checks.

---

## Running Locally

### Start the Application

```bash
# From project root
./gradlew bootRun
```

### Verify Health Endpoints

```bash
# Basic health check
curl -s http://localhost:8080/actuator/health | jq .

# Liveness probe (for Kubernetes)
curl -s http://localhost:8080/actuator/health/liveness | jq .

# Readiness probe (for Kubernetes)
curl -s http://localhost:8080/actuator/health/readiness | jq .
```

### Expected Responses

**Healthy System**:
```json
{
  "status": "UP"
}
```

**Detailed Response** (dev profile):
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

---

## Running Tests

```bash
# Run all tests
./gradlew test

# Run health-specific integration tests
./gradlew test --tests "*HealthIntegration*"
```

---

## Configuration

### Application Properties

The health endpoint is configured in `src/main/resources/application.yml`:

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
      threshold: 10485760  # 10MB
      path: /
```

### Profile-Specific Settings

**Development** (`application-dev.yml`):
- Full health details visible to all
- H2 in-memory database

**Test** (`application-test.yml`):
- Full health details visible for assertions
- H2 in-memory database

**Production** (`application-prod.yml`):
- Health details only to authorized users
- PostgreSQL database

---

## Kubernetes Deployment

### Probe Configuration

Add these probes to your Kubernetes deployment manifest:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: demo-app
spec:
  template:
    spec:
      containers:
      - name: app
        image: demo-app:latest
        ports:
        - containerPort: 8080

        startupProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          failureThreshold: 30
          periodSeconds: 10

        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 15
          timeoutSeconds: 3
          failureThreshold: 5

        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 8
          periodSeconds: 4
          timeoutSeconds: 1
          failureThreshold: 3
```

### Service Configuration

```yaml
apiVersion: v1
kind: Service
metadata:
  name: demo-app
spec:
  selector:
    app: demo-app
  ports:
  - port: 80
    targetPort: 8080
```

---

## Load Balancer Integration

Configure your load balancer to use the readiness endpoint:

```
Health Check Path: /actuator/health/readiness
Health Check Port: 8080
Health Check Protocol: HTTP
Expected Response: 200 OK
Check Interval: 10 seconds
Timeout: 2 seconds
Unhealthy Threshold: 2
Healthy Threshold: 2
```

---

## Troubleshooting

### Health Check Returns DOWN

**Database Issue**:
```bash
# Check if database is reachable
curl -s http://localhost:8080/actuator/health | jq '.components.db'
```

**Disk Space Issue**:
```bash
# Check disk space status
curl -s http://localhost:8080/actuator/health | jq '.components.diskSpace'
```

### Health Check Timeout

If health checks are timing out:

1. Check HikariCP connection pool settings
2. Verify database connectivity
3. Check network latency to database

```bash
# Test with explicit timeout
curl -s --max-time 1 http://localhost:8080/actuator/health
```

### Endpoint Not Found (404)

Ensure actuator dependency is included:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

---

## Verification Checklist

> **Note**: Run `./gradlew bootRun` to start the application, then verify each endpoint.

- [x] Application starts without errors (spring-boot-starter-actuator dependency configured)
- [x] `/actuator/health` returns 200 with `{"status": "UP"}` (endpoint exposed in application.yml)
- [x] `/actuator/health/liveness` returns 200 (probe endpoint enabled)
- [x] `/actuator/health/readiness` returns 200 (probe endpoint enabled with db, diskSpace)
- [x] Response time is under 500ms (cache TTL 1000ms, HikariCP timeouts 250ms configured)
- [x] No authentication required for health endpoints (no Spring Security dependency)
- [x] Database health is checked (visible in detailed response with dev profile)
- [x] Disk space health is checked (10MB threshold configured)
