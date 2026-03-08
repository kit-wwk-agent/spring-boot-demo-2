# Contract: Health Endpoint

**Feature**: 007-actuator-health
**Version**: 1.0.0
**Date**: 2026-03-08

## Endpoint Overview

| Property | Value |
|----------|-------|
| Path | `/actuator/health` |
| Method | GET |
| Authentication | None required |
| Content-Type | application/json |

## Request

### Headers

| Header | Required | Description |
|--------|----------|-------------|
| Accept | No | If present, should include `application/json` |

### Query Parameters

None

### Request Body

None

## Response

### Success Response (All Components Healthy)

**Status Code**: 200 OK

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "status": "UP"
}
```

### Failure Response (Any Component Unhealthy)

**Status Code**: 503 Service Unavailable

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "status": "DOWN"
}
```

## Status Values

| Status | HTTP Code | Description |
|--------|-----------|-------------|
| UP | 200 | All health indicators report healthy |
| DOWN | 503 | One or more critical health indicators failed |
| OUT_OF_SERVICE | 503 | Component taken out of service |
| UNKNOWN | 200 | Health status cannot be determined |

## Health Groups

### Liveness Probe

| Property | Value |
|----------|-------|
| Path | `/actuator/health/liveness` |
| Purpose | Kubernetes liveness probe |
| Components | livenessState |

### Readiness Probe

| Property | Value |
|----------|-------|
| Path | `/actuator/health/readiness` |
| Purpose | Kubernetes readiness probe |
| Components | readinessState, db, diskSpace |

## Performance Contract

| Metric | Requirement |
|--------|-------------|
| Response Time | < 500ms (p99) |
| Availability | As available as the application itself |
| Caching | 1000ms TTL to prevent thundering herd |

## Example Requests

### Basic Health Check

```bash
curl -X GET http://localhost:8080/actuator/health
```

**Response (healthy)**:
```json
{"status":"UP"}
```

### Liveness Probe

```bash
curl -X GET http://localhost:8080/actuator/health/liveness
```

**Response**:
```json
{"status":"UP"}
```

### Readiness Probe

```bash
curl -X GET http://localhost:8080/actuator/health/readiness
```

**Response (healthy)**:
```json
{"status":"UP"}
```

**Response (database down)**:
```json
{"status":"DOWN"}
```

## Integration Notes

### Load Balancer Configuration

Configure health check URL: `http://<host>:8080/actuator/health`
- Expected success codes: 200
- Expected failure codes: 503
- Recommended interval: 10-30 seconds
- Recommended timeout: 5 seconds

### Kubernetes Configuration

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
```

## Backward Compatibility

This endpoint follows Spring Boot Actuator conventions and maintains compatibility with:
- Spring Boot 2.x health response format
- Spring Boot 3.x health response format
- Standard HTTP health check patterns

## Error Handling

| Scenario | HTTP Status | Response Body |
|----------|-------------|---------------|
| Database unreachable | 503 | `{"status":"DOWN"}` |
| Disk space below threshold | 503 | `{"status":"DOWN"}` |
| Application starting | 503 | `{"status":"DOWN"}` (readiness) |
| Timeout during health check | 503 | `{"status":"DOWN"}` |
