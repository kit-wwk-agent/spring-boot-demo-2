# Data Model: API Rate Limiting

**Feature**: 001-api-rate-limiting | **Date**: 2026-02-26

## Overview

This feature uses in-memory data structures only. No database entities are required. Rate limit counters are transient and reset on application restart.

## Entities

### RateLimitCounter

Tracks the number of requests from a specific client IP within the current time window.

| Field | Type | Description |
|-------|------|-------------|
| `clientIp` | `String` | Client IP address (key in ConcurrentHashMap) |
| `requestCount` | `int` | Number of requests in current window |
| `windowStartTime` | `long` | Epoch milliseconds when the current window started |

**Invariants**:
- `requestCount` >= 0
- `windowStartTime` must be a valid epoch timestamp

**Operations**:
- `increment()`: Atomically increment request count
- `isExpired(windowDurationMs)`: Check if window has expired
- `reset(currentTime)`: Reset counter for new window

### RateLimitProperties (Configuration)

Configuration values bound from `application.properties`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ratelimit.requests-per-minute` | `int` | `60` | Maximum requests allowed per minute per client IP |
| `ratelimit.window-duration-ms` | `long` | `60000` | Rate limit window duration in milliseconds |
| `ratelimit.enabled` | `boolean` | `true` | Enable/disable rate limiting globally |

### RateLimitResponse

Response body returned when rate limit is exceeded.

| Field | Type | Description |
|-------|------|-------------|
| `error` | `String` | Error type: "Too Many Requests" |
| `message` | `String` | Human-readable message |
| `retryAfter` | `long` | Seconds until rate limit resets |

**HTTP Headers** (on 429 response):
- `Retry-After`: seconds until client can retry

## State Transitions

```
┌─────────────────────────────────────────────────────────────────┐
│                     RateLimitCounter States                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│    ┌───────────┐     request      ┌───────────────┐             │
│    │           │ ──────────────▶ │               │             │
│    │  (null)   │   first req     │   TRACKING    │             │
│    │           │   from IP       │ count: 1      │             │
│    └───────────┘                 │ window: now   │             │
│                                   └───────┬───────┘             │
│                                           │                      │
│                                           │ request              │
│                                           │ (count < limit)      │
│                                           ▼                      │
│                                   ┌───────────────┐             │
│                                   │   TRACKING    │             │
│                                   │ count: n+1   │ ◀──┐        │
│                                   │ window: same │ ───┘        │
│                                   └───────┬───────┘  request    │
│                                           │       (count < limit)│
│                                           │                      │
│                                           │ request              │
│                                           │ (count >= limit)     │
│                                           ▼                      │
│                                   ┌───────────────┐             │
│                                   │   LIMITED     │             │
│                                   │ → HTTP 429    │             │
│                                   └───────┬───────┘             │
│                                           │                      │
│                                           │ window expires       │
│                                           ▼                      │
│                                   ┌───────────────┐             │
│                                   │   TRACKING    │             │
│                                   │ count: 1      │             │
│                                   │ window: now   │             │
│                                   └───────────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Validation Rules

| Entity | Rule | Error |
|--------|------|-------|
| RateLimitProperties | `requests-per-minute` > 0 | Configuration error at startup |
| RateLimitProperties | `window-duration-ms` > 0 | Configuration error at startup |
| Client IP | Must be valid IPv4 or IPv6 | Allow request (fail-open) if invalid/missing |

## Storage Strategy

- **Type**: In-memory `ConcurrentHashMap<String, RateLimitCounter>`
- **Key**: Client IP address as string
- **Thread Safety**: Atomic `compute()` operations
- **Cleanup**: `ScheduledExecutorService` removes expired entries every 60 seconds
- **Persistence**: None - counters reset on application restart
