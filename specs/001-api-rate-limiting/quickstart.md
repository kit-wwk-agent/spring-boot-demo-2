# Quickstart: API Rate Limiting

**Feature**: 001-api-rate-limiting | **Date**: 2026-02-26

## Prerequisites

- Java 21 (LTS)
- Gradle 8.x

## Quick Test

### 1. Build and Run

```bash
./gradlew bootRun
```

### 2. Test Rate Limiting

```bash
# Make requests to an API endpoint
for i in {1..65}; do
  echo "Request $i: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/test)"
done
```

Expected: First 60 requests return 200, requests 61-65 return 429.

### 3. Check Rate Limit Response

```bash
# After being rate limited
curl -i http://localhost:8080/api/test
```

Expected response:
```
HTTP/1.1 429 Too Many Requests
Retry-After: 45
Content-Type: application/json

{"error":"Too Many Requests","message":"Rate limit exceeded. Please retry after 45 seconds.","retryAfter":45}
```

### 4. Verify Actuator Excluded

```bash
# Actuator endpoints should always work
curl http://localhost:8080/actuator/health
```

Expected: Always returns health status, never rate limited.

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Rate limiting configuration
ratelimit.requests-per-minute=60
ratelimit.enabled=true
```

| Property | Default | Description |
|----------|---------|-------------|
| `ratelimit.requests-per-minute` | 60 | Max requests per minute per IP |
| `ratelimit.enabled` | true | Enable/disable rate limiting |

## Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew test --tests "*IntegrationTest"
```

## Project Structure

```
src/main/java/com/example/demo/
├── config/
│   └── RateLimitProperties.java    # Configuration binding
├── filter/
│   └── RateLimitFilter.java        # Servlet filter
└── ratelimit/
    ├── RateLimitService.java       # Rate limiting logic
    └── RateLimitCounter.java       # Per-IP counter

src/test/java/com/example/demo/
├── filter/
│   └── RateLimitFilterTest.java
└── integration/
    └── RateLimitIntegrationTest.java
```

## Troubleshooting

### Rate limiting not working

1. Check `ratelimit.enabled=true` in configuration
2. Verify requests are to `/api/*` endpoints
3. Check application logs for filter registration

### Always getting 429

1. Wait 60 seconds for window to reset
2. Check configured limit: `ratelimit.requests-per-minute`
3. Verify no other clients share your IP (NAT/proxy)

### IP detection issues

If behind a proxy, ensure the proxy sets `X-Forwarded-For` header correctly.
