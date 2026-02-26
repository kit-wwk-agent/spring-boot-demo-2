# Implementation Plan: API Rate Limiting

**Branch**: `001-api-rate-limiting` | **Date**: 2026-02-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-api-rate-limiting/spec.md`

## Summary

Implement a Spring Boot servlet filter that enforces configurable requests-per-minute rate limiting per client IP address on `/api/*` endpoints. Requests exceeding the limit receive HTTP 429 with Retry-After header. Health/actuator endpoints are excluded.

## Technical Context

**Language/Version**: Java 21 (LTS)
**Framework**: Spring Boot 3.4.3
**Build Tool**: Gradle 8.x with Kotlin DSL
**Primary Dependencies**: spring-boot-starter-web, spring-boot-starter-actuator, spring-boot-starter-data-jpa
**Storage**: In-memory (ConcurrentHashMap for rate limit counters); H2 for dev, PostgreSQL for prod (existing, not used for rate limiting)
**Testing**: JUnit 5 via spring-boot-starter-test
**Target Platform**: JVM 21 server deployment
**Project Type**: Web service / REST API
**Performance Goals**: Rate limit check completes within 50ms (per SC-001); no delay for requests within limit (SC-004)
**Constraints**: In-memory rate limiting only (single instance); rate limit state not persisted across restarts
**Scale/Scope**: Single application instance; configurable 30-100+ requests/minute per IP

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: PASS (No project-specific constitution configured)

The project constitution (`.specify/memory/constitution.md`) contains only template placeholders. No custom principles or gates are defined. Proceeding with standard Spring Boot best practices:

- **Simplicity**: Single filter class with in-memory storage; no over-engineering
- **Testing**: Unit tests for filter logic; integration tests for endpoint behavior
- **Configurability**: Use Spring Boot's standard `application.properties`/`application.yml` configuration

## Project Structure

### Documentation (this feature)

```text
specs/001-api-rate-limiting/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── rate-limit-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java          # Existing entry point
│   │   ├── config/
│   │   │   └── RateLimitProperties.java  # Configuration binding
│   │   ├── filter/
│   │   │   └── RateLimitFilter.java      # Servlet filter implementation
│   │   └── ratelimit/
│   │       ├── RateLimitService.java     # Rate limiting logic
│   │       └── RateLimitCounter.java     # Per-IP counter POJO
│   └── resources/
│       └── application.properties        # Rate limit configuration
└── test/
    └── java/com/example/demo/
        ├── filter/
        │   └── RateLimitFilterTest.java  # Unit tests
        └── integration/
            └── RateLimitIntegrationTest.java  # Integration tests
```

**Structure Decision**: Standard Spring Boot single-module Maven/Gradle structure using `src/main/java` and `src/test/java`. Filter registered via Spring component scanning. Configuration externalized to `application.properties`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations. Design follows simplest viable approach:*
- Single filter class with inline rate limiting logic
- In-memory storage (ConcurrentHashMap) - no external dependencies
- Standard Spring Boot configuration binding
