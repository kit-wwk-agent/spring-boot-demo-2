# API Contract: Rate Limiting

**Feature**: 001-api-rate-limiting | **Version**: 1.0.0

## Overview

This contract defines the rate limiting behavior for all `/api/*` endpoints. Rate limiting is transparent to successful requests but returns standard HTTP 429 responses when limits are exceeded.

## Request Behavior

### Rate Limited Endpoints

| Pattern | Description | Rate Limited |
|---------|-------------|--------------|
| `/api/**` | All API endpoints | Yes |
| `/actuator/**` | Health and metrics | No |
| `/health` | Health endpoint | No |

### Client Identification

The client is identified by IP address, extracted in order of precedence:

1. `X-Forwarded-For` header (first IP in comma-separated list)
2. `request.getRemoteAddr()` (direct connection)

## Response Contracts

### Successful Request (Within Rate Limit)

No additional headers are added. Request proceeds to the target endpoint.

### Rate Limited Response

**Status Code**: `429 Too Many Requests`

**Headers**:

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Retry-After` | integer | Yes | Seconds until rate limit window resets |
| `Content-Type` | string | Yes | `application/json` |

**Body**:

```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please retry after {seconds} seconds.",
  "retryAfter": 45
}
```

**Response Schema** (JSON Schema):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["error", "message", "retryAfter"],
  "properties": {
    "error": {
      "type": "string",
      "const": "Too Many Requests"
    },
    "message": {
      "type": "string",
      "pattern": "^Rate limit exceeded\\."
    },
    "retryAfter": {
      "type": "integer",
      "minimum": 0,
      "maximum": 60,
      "description": "Seconds until rate limit resets"
    }
  }
}
```

## Configuration Contract

Rate limiting is configured via `application.properties` or `application.yml`.

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `ratelimit.requests-per-minute` | integer | `60` | 1-10000 | Max requests per minute per IP |
| `ratelimit.enabled` | boolean | `true` | - | Enable/disable rate limiting |

**Example** (`application.properties`):

```properties
ratelimit.requests-per-minute=100
ratelimit.enabled=true
```

**Example** (`application.yml`):

```yaml
ratelimit:
  requests-per-minute: 100
  enabled: true
```

## Behavior Contract

### Rate Limit Window

- **Duration**: Fixed 60-second windows
- **Reset**: Counter resets when window expires
- **Algorithm**: Fixed window (not sliding)

### Edge Cases

| Scenario | Behavior |
|----------|----------|
| Missing client IP | Request allowed (fail-open) |
| Shared IP (NAT/proxy) | All clients behind IP share the limit |
| Application restart | All counters reset to zero |
| Clock changes | Based on system monotonic time |

## Test Scenarios

### Scenario 1: Normal Operation

```
Given: Client has made 0 requests
When: Client makes request to /api/users
Then: Request succeeds with normal response
```

### Scenario 2: At Limit

```
Given: Client has made 59 requests in current minute (limit=60)
When: Client makes request to /api/users
Then: Request succeeds with normal response
```

### Scenario 3: Exceed Limit

```
Given: Client has made 60 requests in current minute (limit=60)
When: Client makes request to /api/users
Then: Response is 429 with Retry-After header
```

### Scenario 4: Actuator Excluded

```
Given: Client is rate limited on /api/*
When: Client makes request to /actuator/health
Then: Request succeeds (actuator not rate limited)
```

### Scenario 5: Window Reset

```
Given: Client was rate limited
When: 60 seconds elapse
Then: Client's next request succeeds
```
