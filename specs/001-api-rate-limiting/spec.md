# Feature Specification: API Rate Limiting

**Feature Branch**: `001-api-rate-limiting`
**Created**: 2026-02-26
**Status**: Draft
**Input**: User description: "Add rate limiting filter enforcing configurable max requests-per-minute per client IP with HTTP 429 response"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rate Limited Feedback (Priority: P1)

As an API consumer, I want to receive clear feedback when I am rate limited so that I can understand why my request was rejected and know when to retry.

**Why this priority**: This is the core value proposition - without clear feedback, rate limiting would frustrate users and provide no guidance for compliance. This directly impacts API usability.

**Independent Test**: Can be fully tested by making rapid requests to an API endpoint and verifying the response includes rate limit status and retry timing information.

**Acceptance Scenarios**:

1. **Given** I have made 60 requests within the current minute, **When** I make the 61st request to `/api/*`, **Then** I receive HTTP 429 Too Many Requests with a Retry-After header indicating seconds until limit resets.
2. **Given** I am rate limited, **When** the rate limit window resets (60 seconds), **Then** my subsequent request succeeds normally.
3. **Given** I make a request, **When** my request count is below the limit, **Then** my request processes normally without any rate limiting interference.

---

### User Story 2 - Configurable Rate Limit (Priority: P2)

As an operator, I want to configure the rate limit threshold without code changes so that I can tune system protection based on observed traffic patterns and capacity.

**Why this priority**: Configurability enables operators to adapt the system to real-world conditions without redeployment, essential for production operations but secondary to the core rate limiting functionality.

**Independent Test**: Can be tested by changing the configuration value and verifying the new limit is enforced without application code changes.

**Acceptance Scenarios**:

1. **Given** the configuration is set to 100 requests per minute, **When** a client makes their 101st request within a minute, **Then** they receive HTTP 429.
2. **Given** the configuration is set to 30 requests per minute, **When** a client makes their 31st request within a minute, **Then** they receive HTTP 429.
3. **Given** no explicit configuration is provided, **When** the application starts, **Then** the default limit of 60 requests per minute is applied.

---

### User Story 3 - Excluded Health Endpoints (Priority: P3)

As an operator, I want health and actuator endpoints to be excluded from rate limiting so that monitoring systems can always check application health without being blocked.

**Why this priority**: Health checks are critical for infrastructure management but are a supporting concern - the core rate limiting must work first.

**Independent Test**: Can be tested by making unlimited requests to actuator endpoints and verifying they always succeed regardless of rate limit status.

**Acceptance Scenarios**:

1. **Given** a client is rate limited on `/api/*` endpoints, **When** they request `/actuator/health`, **Then** the health endpoint responds successfully.
2. **Given** a monitoring system makes frequent health check requests, **When** these requests exceed the normal rate limit, **Then** all health check requests succeed.

---

### Edge Cases

- What happens when multiple clients share the same IP address (NAT/proxy)? Each apparent IP is treated as one client; shared IPs share the rate limit.
- How does the system handle clock skew or time zone differences? Rate windows are based on server time, resetting every 60 seconds from first request.
- What happens when the application restarts? Rate limit counters reset; in-flight request counts are not persisted.
- How are IPv6 addresses handled? Both IPv4 and IPv6 addresses are supported as client identifiers.
- What happens if the client IP cannot be determined? Requests without identifiable client IP are allowed (fail-open for edge cases like malformed headers).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST enforce rate limiting on all `/api/*` endpoints.
- **FR-002**: System MUST track request counts per unique client IP address.
- **FR-003**: System MUST return HTTP 429 Too Many Requests when a client exceeds the configured limit.
- **FR-004**: System MUST include a Retry-After header in 429 responses indicating seconds until the rate limit window resets.
- **FR-005**: System MUST reset rate limit counters every 60 seconds.
- **FR-006**: System MUST exclude `/actuator/*` and `/health` endpoints from rate limiting.
- **FR-007**: System MUST support configuration of the requests-per-minute limit via application configuration.
- **FR-008**: System MUST apply a default limit of 60 requests per minute when no configuration is specified.
- **FR-009**: System MUST NOT block or delay requests that are within the rate limit.
- **FR-010**: System MUST handle both IPv4 and IPv6 client addresses.

### Key Entities

- **Rate Limit Counter**: Tracks the number of requests from a specific client IP within the current time window. Key attributes: client IP, request count, window start time.
- **Rate Limit Configuration**: Defines the maximum allowed requests per minute. Key attributes: requests-per-minute threshold.
- **Rate Limit Response**: The error response returned when limit is exceeded. Key attributes: HTTP status 429, Retry-After header value.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: API consumers exceeding the rate limit receive HTTP 429 response within 50ms of request arrival.
- **SC-002**: Rate limit configuration changes take effect without application code changes or recompilation.
- **SC-003**: Health and actuator endpoints remain accessible 100% of the time regardless of rate limit status.
- **SC-004**: 99.9% of requests within the rate limit are processed without delay from the rate limiting mechanism.
- **SC-005**: Retry-After header accurately reflects the remaining time in the current rate window (within 1 second accuracy).
- **SC-006**: Rate limit counters reset correctly every 60 seconds, allowing clients to resume normal operation.

## Assumptions

- Client IP is determined from the request's remote address (standard HTTP handling).
- Rate limiting is applied in-memory; distributed/clustered rate limiting across multiple application instances is out of scope.
- The 60-second rate window is fixed and uses a sliding window or fixed window approach (implementation detail).
- Configuration reloading at runtime without restart is not required; configuration is read at application startup.
- Only HTTP GET methods were specified, but rate limiting will apply to all HTTP methods on `/api/*` endpoints for consistent protection.
