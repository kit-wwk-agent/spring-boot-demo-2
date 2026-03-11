# API Contract: Health Endpoints

**Feature**: 008-actuator-health | **Version**: 1.0 | **Date**: 2026-03-11

## Base Path

```
/actuator
```

---

## Endpoints

### GET /actuator/health

Returns aggregate health status of the application.

**Authentication**: None required

**Request**:
```http
GET /actuator/health HTTP/1.1
Host: localhost:8080
Accept: application/json
```

**Response (200 OK - Healthy)**:
```json
{
  "status": "UP"
}
```

**Response (503 Service Unavailable - Unhealthy)**:
```json
{
  "status": "DOWN"
}
```

**Response Headers**:
```
Content-Type: application/vnd.spring-boot.actuator.v3+json
```

**Status Codes**:
| Code | Condition |
|------|-----------|
| 200 | All health indicators report UP |
| 503 | Any critical health indicator reports DOWN |

**Performance**: Response time < 500ms (p99)

---

### GET /actuator/health/liveness

Returns liveness probe status for Kubernetes.

**Use Case**: Kubernetes `livenessProbe` - determines if pod should be restarted.

**Authentication**: None required

**Request**:
```http
GET /actuator/health/liveness HTTP/1.1
Host: localhost:8080
Accept: application/json
```

**Response (200 OK)**:
```json
{
  "status": "UP"
}
```

**Response (503 Service Unavailable)**:
```json
{
  "status": "DOWN"
}
```

**Included Indicators**:
- `livenessState` only (no external dependencies)

---

### GET /actuator/health/readiness

Returns readiness probe status for Kubernetes.

**Use Case**: Kubernetes `readinessProbe` - determines if pod should receive traffic.

**Authentication**: None required

**Request**:
```http
GET /actuator/health/readiness HTTP/1.1
Host: localhost:8080
Accept: application/json
```

**Response (200 OK)**:
```json
{
  "status": "UP"
}
```

**Response (503 Service Unavailable)**:
```json
{
  "status": "OUT_OF_SERVICE"
}
```

**Included Indicators**:
- `readinessState`
- `db` (database connectivity)
- `diskSpace` (disk availability)

---

## Health Status Values

| Status | HTTP Code | Description |
|--------|-----------|-------------|
| UP | 200 | Component is healthy |
| DOWN | 503 | Component is unhealthy |
| OUT_OF_SERVICE | 503 | Component is refusing traffic |
| UNKNOWN | 200 | Health cannot be determined |

---

## Response Schema

### HealthResponse

```yaml
type: object
required:
  - status
properties:
  status:
    type: string
    enum: [UP, DOWN, OUT_OF_SERVICE, UNKNOWN]
    description: Aggregate health status
  components:
    type: object
    additionalProperties:
      $ref: '#/components/schemas/HealthComponent'
    description: Individual health indicators (shown when authorized)
```

### HealthComponent

```yaml
type: object
required:
  - status
properties:
  status:
    type: string
    enum: [UP, DOWN, OUT_OF_SERVICE, UNKNOWN]
  details:
    type: object
    additionalProperties: true
    description: Component-specific details (shown when authorized)
```

---

## Kubernetes Integration Example

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: app
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 10
      timeoutSeconds: 1
      failureThreshold: 3
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 5
      periodSeconds: 5
      timeoutSeconds: 1
      failureThreshold: 3
```

---

## Load Balancer Integration Example

**AWS ALB Health Check**:
- Path: `/actuator/health`
- Protocol: HTTP
- Port: 8080
- Healthy threshold: 2
- Unhealthy threshold: 3
- Timeout: 5 seconds
- Interval: 30 seconds
- Success codes: 200

---

## Notes

- Content-Type is `application/vnd.spring-boot.actuator.v3+json` (JSON-compatible)
- Details are hidden for unauthenticated requests (`show-details: when_authorized`)
- Cache TTL is 1000ms to prevent excessive polling load
