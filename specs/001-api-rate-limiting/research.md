# API Rate Limiting Research

**Feature**: 001-api-rate-limiting | **Date**: 2026-02-26

## 1. Spring Boot Servlet Filter Implementation

### Decision: Use `OncePerRequestFilter` with `FilterRegistrationBean` for URL pattern configuration

### Rationale

`OncePerRequestFilter` is the recommended base class for implementing rate limiting filters in Spring Boot because:

1. **Single Execution Guarantee**: Guarantees a single execution per request dispatch, even when a request passes through multiple servlets or filter chains
2. **Convenience Methods**: Provides `doFilterInternal()` with typed `HttpServletRequest` and `HttpServletResponse` parameters
3. **Built-in Skip Logic**: Allows overriding `shouldNotFilter()` method for additional exclusion logic beyond URL patterns

For URL pattern configuration, `FilterRegistrationBean` is the preferred approach for fine-grained control over URL patterns and filter ordering.

### Alternatives Considered

| Approach | Pros | Cons |
|----------|------|------|
| `@WebFilter` annotation | Simple, declarative | Less flexible, requires `@ServletComponentScan` |
| Standard `Filter` interface | Maximum control | More boilerplate, no single-execution guarantee |
| Spring Security filter chain | Integrates with security | Overkill for simple rate limiting |
| `HandlerInterceptor` | Access to handler metadata | Runs after filter chain, less suitable for early rejection |

---

## 2. In-Memory Rate Limiting with ConcurrentHashMap

### Decision: Use `ConcurrentHashMap` with atomic `compute()` operations and `ScheduledExecutorService` for cleanup

### Rationale

**Thread Safety with Atomic Operations**

`ConcurrentHashMap.compute()` provides atomic per-key operations, ideal for request counters:

- The entire method invocation is performed atomically
- Multiple threads can update different keys concurrently
- Internal concurrency controls ensure the remapping function is applied atomically

**Cleanup Strategy**

Use `ScheduledExecutorService` for periodic cleanup of stale entries:

- Schedule cleanup task at fixed intervals (e.g., every minute)
- Iterate through entries and remove those past their window expiration
- More efficient than inline cleanup on every request

### Alternatives Considered

| Approach | Pros | Cons |
|----------|------|------|
| `synchronized` blocks | Simple to understand | Poor performance under contention |
| `AtomicInteger` per client | Very fast | Complex lifecycle management |
| Guava `Cache` with expiry | Built-in eviction | Additional dependency |
| Caffeine cache | High performance, automatic eviction | Additional dependency |

---

## 3. Fixed Window vs Sliding Window Rate Limiting

### Decision: Use Fixed Window algorithm for single-instance deployment

### Rationale

**Fixed Window** is the appropriate choice for this use case because:

1. **Simplicity**: Significantly easier to implement correctly
2. **Lower Memory**: Requires only a counter and window start timestamp per client
3. **Lower CPU**: Simple comparison operations vs. complex timestamp tracking
4. **Sufficient for Single Instance**: The "boundary burst" problem is acceptable for non-critical rate limiting

**How Fixed Window Works**:
- Count requests within a strict time window (e.g., 60 seconds)
- When the window expires, reset the counter
- If count exceeds limit within window, reject request

**Known Limitation - Boundary Burst Problem**:
With 100 requests/minute limit, a client could theoretically make 200 requests in 2 seconds across a window boundary. For single-instance deployments with reasonable limits, this edge case is acceptable.

### Alternatives Considered

| Algorithm | Complexity | Memory | Accuracy | Best For |
|-----------|------------|--------|----------|----------|
| Fixed Window | Low | Low | Moderate | Simple APIs, single instance |
| Sliding Window Log | High | High | High | Critical APIs |
| Sliding Window Counter | Medium | Medium | High | Balanced requirements |
| Token Bucket | Medium | Low | High | Burst-friendly APIs |
| Leaky Bucket | Medium | Low | High | Smooth rate enforcement |

---

## 4. Client IP Address Extraction

### Decision: Check `X-Forwarded-For` header first, fall back to `request.getRemoteAddr()`

### Rationale

**X-Forwarded-For Header Format**:
```
X-Forwarded-For: client, proxy1, proxy2
```
The leftmost IP is the original client; each proxy appends the IP it received the request from.

**Recommended Extraction Logic**:
1. Check for `X-Forwarded-For` header
2. If present, extract the first (leftmost) IP address
3. If absent, use `request.getRemoteAddr()`

**Security Considerations**:

The `X-Forwarded-For` header can be spoofed by malicious clients. Mitigations include:

1. **Trust only when behind a known proxy**: Only read forwarded headers when deployed behind a trusted reverse proxy
2. **Validate IP format**: Ensure extracted value is a valid IP address
3. **Fail-open for edge cases**: If IP cannot be determined, allow the request (per spec edge case)

---

## 5. HTTP 429 Response and Retry-After Header

### Decision: Return HTTP 429 with `Retry-After` header in seconds format

### Rationale

**RFC 6585 Requirements**:
- HTTP 429 indicates the user has sent too many requests in a given time period
- Response representations SHOULD include details explaining the condition
- Response MAY include a `Retry-After` header

**Retry-After Header Formats**:
1. **Seconds** (recommended): `Retry-After: 60`
2. **HTTP-date**: `Retry-After: Wed, 21 Oct 2025 07:28:00 GMT`

The seconds format is simpler and avoids timezone/clock synchronization issues.

**Response Body**:

Include a JSON body explaining the error:
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please retry after 60 seconds.",
  "retryAfter": 60
}
```

---

## Summary of Decisions

| Topic | Decision | Key Rationale |
|-------|----------|---------------|
| Filter Implementation | `OncePerRequestFilter` + `FilterRegistrationBean` | Single execution guarantee, clean URL pattern config |
| Thread-Safe Storage | `ConcurrentHashMap` with `compute()` | Atomic per-key operations, high concurrency |
| Cleanup Strategy | `ScheduledExecutorService` | Background cleanup avoids inline overhead |
| Rate Limiting Algorithm | Fixed Window | Simple, low overhead, sufficient for single instance |
| Client Identification | X-Forwarded-For with fallback | Proxy-aware, simple implementation |
| Error Response | HTTP 429 + Retry-After (seconds) | RFC compliant, clear client guidance |
