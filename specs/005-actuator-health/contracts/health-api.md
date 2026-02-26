# Health API Contract

**Version**: 1.0.0
**Base Path**: `/actuator`

---

## Endpoints

### GET /actuator/health

Returns the overall health status of the application.

**Authentication**: None required (unauthenticated access)

#### Request

```http
GET /actuator/health HTTP/1.1
Host: localhost:8080
Accept: application/json
```

#### Response

**Success (200 OK)** - When all components are healthy:

```json
{
  "status": "UP"
}
```

**Failure (503 Service Unavailable)** - When any component is unhealthy:

```json
{
  "status": "DOWN"
}
```

**Detailed Response** (dev profile or authorized users):

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
        "free": 375000000000,
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

#### Response Headers

| Header | Value |
|--------|-------|
| Content-Type | application/json |
| Cache-Control | no-cache, no-store |

---

### GET /actuator/health/liveness

Returns the liveness state for Kubernetes liveness probes.

**Purpose**: Determines if the JVM process is alive and should continue running.

**Authentication**: None required

#### Request

```http
GET /actuator/health/liveness HTTP/1.1
Host: localhost:8080
Accept: application/json
```

#### Response

**Success (200 OK)**:

```json
{
  "status": "UP"
}
```

**Failure (503 Service Unavailable)**:

```json
{
  "status": "DOWN"
}
```

#### Notes

- Does NOT check external dependencies (database, disk space)
- Failure indicates the container should be restarted
- Should always respond quickly (internal state only)

---

### GET /actuator/health/readiness

Returns the readiness state for Kubernetes readiness probes.

**Purpose**: Determines if the application can accept traffic.

**Authentication**: None required

#### Request

```http
GET /actuator/health/readiness HTTP/1.1
Host: localhost:8080
Accept: application/json
```

#### Response

**Success (200 OK)**:

```json
{
  "status": "UP"
}
```

**Failure (503 Service Unavailable)**:

```json
{
  "status": "DOWN"
}
```

#### Notes

- Checks external dependencies: database, disk space
- Failure removes pod from load balancer (does NOT restart)
- May take longer to respond due to dependency checks

---

## Status Codes

| Status Code | Meaning | When Used |
|-------------|---------|-----------|
| 200 OK | Application/component is healthy | status = UP |
| 503 Service Unavailable | Application/component is unhealthy | status = DOWN |

---

## Health Status Values

| Status | Description |
|--------|-------------|
| `UP` | Component is functioning correctly |
| `DOWN` | Component is not functioning |
| `UNKNOWN` | Component status cannot be determined |
| `OUT_OF_SERVICE` | Component is explicitly out of service |

---

## Health Groups

| Group | Endpoint | Included Indicators | Use Case |
|-------|----------|-------------------|----------|
| `liveness` | `/actuator/health/liveness` | livenessState | Kubernetes liveness probe |
| `readiness` | `/actuator/health/readiness` | readinessState, db, diskSpace | Kubernetes readiness probe, load balancer |

---

## Performance Requirements

| Metric | Requirement | Reference |
|--------|-------------|-----------|
| Response Time | < 500ms (95th percentile) | FR-006 |
| Cache TTL | 1000ms | Prevents rapid repeated checks |
| DB Check Timeout | 250ms | HikariCP configuration |

---

## Example cURL Commands

### Basic Health Check

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

### Liveness Probe

```bash
curl -s http://localhost:8080/actuator/health/liveness | jq .
```

### Readiness Probe

```bash
curl -s http://localhost:8080/actuator/health/readiness | jq .
```

### Check with Timeout (useful for testing)

```bash
curl -s --max-time 1 http://localhost:8080/actuator/health
```

---

## Kubernetes Probe Configuration

```yaml
# Recommended probe configuration for deployment manifests
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

---

## Error Scenarios

### Database Unavailable

**Response**: 503 Service Unavailable

```json
{
  "status": "DOWN"
}
```

With details (dev profile):

```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "error": "org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### Disk Space Low

**Response**: 503 Service Unavailable

```json
{
  "status": "DOWN"
}
```

With details (dev profile):

```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "DOWN",
      "details": {
        "total": 499963174912,
        "free": 5000000,
        "threshold": 10485760,
        "path": "/.",
        "exists": true
      }
    }
  }
}
```
