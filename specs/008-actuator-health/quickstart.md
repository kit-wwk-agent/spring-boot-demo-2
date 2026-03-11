# Quickstart: Actuator Health Endpoint

**Feature**: 008-actuator-health | **Date**: 2026-03-11

## Prerequisites

- Java 21+
- Gradle 8.x

## Running the Application

```bash
# Start the application
./gradlew bootRun

# Or with dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Testing Health Endpoints

### Basic Health Check

```bash
# Check overall health
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Kubernetes Probes

```bash
# Liveness probe (is the app alive?)
curl http://localhost:8080/actuator/health/liveness
# Expected: {"status":"UP"}

# Readiness probe (can the app accept traffic?)
curl http://localhost:8080/actuator/health/readiness
# Expected: {"status":"UP"}
```

### Detailed Health Information

In the test profile or when authenticated, you can see component details:

```bash
# With verbose output
curl -s http://localhost:8080/actuator/health | jq .
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run only health tests
./gradlew test --tests "*Health*"

# Run specific test class
./gradlew test --tests "com.example.demo.health.BasicHealthCheckTest"
./gradlew test --tests "com.example.demo.health.DatabaseHealthCheckTest"
./gradlew test --tests "com.example.demo.health.DiskSpaceHealthCheckTest"
./gradlew test --tests "com.example.demo.integration.HealthEndpointIntegrationTest"
```

## Configuration

### Key Configuration (application.yml)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized  # Hide details from unauthenticated users
      probes:
        enabled: true                # Enable /health/liveness and /health/readiness
      cache:
        time-to-live: 1000ms        # Cache health results for 1 second
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db, diskSpace
  health:
    diskspace:
      threshold: 10485760            # 10MB minimum free space
```

### Test Profile (application-test.yml)

```yaml
management:
  endpoint:
    health:
      show-details: always          # Show all details in tests
      show-components: always
```

## Health Indicators

| Indicator | Endpoint | Description |
|-----------|----------|-------------|
| `db` | /actuator/health | Database connectivity via DataSource |
| `diskSpace` | /actuator/health | Disk space above threshold |
| `livenessState` | /actuator/health/liveness | Application alive state |
| `readinessState` | /actuator/health/readiness | Application ready state |

## Troubleshooting

### Health Returns DOWN

1. **Database DOWN**: Check database connectivity
   ```bash
   # Check logs for connection errors
   ./gradlew bootRun 2>&1 | grep -i "datasource\|connection"
   ```

2. **Disk Space DOWN**: Check available disk space
   ```bash
   df -h /
   # Threshold is 10MB - ensure free space exceeds this
   ```

### Endpoint Not Found (404)

Verify actuator endpoints are exposed:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
```

### Response Time > 500ms

1. Check database connection pool settings
2. Increase cache TTL to reduce health check frequency
3. Verify network latency to database

## Integration Examples

### Kubernetes Deployment

```yaml
spec:
  containers:
  - name: app
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 10
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 5
      periodSeconds: 5
```

### Docker Health Check

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

## Files Overview

| File | Purpose |
|------|---------|
| `application.yml` | Health endpoint configuration |
| `application-test.yml` | Test profile with full details exposed |
| `BasicHealthCheckTest.java` | Tests for basic health response |
| `DatabaseHealthCheckTest.java` | Tests for database health indicator |
| `DiskSpaceHealthCheckTest.java` | Tests for disk space indicator |
| `HealthEndpointIntegrationTest.java` | End-to-end integration tests |
