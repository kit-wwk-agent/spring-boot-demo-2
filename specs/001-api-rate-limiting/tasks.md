# Tasks: API Rate Limiting

**Input**: Design documents from `/specs/001-api-rate-limiting/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/rate-limit-api.md

**Tests**: Included (plan.md specifies unit tests for filter logic and integration tests for endpoint behavior)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify project structure and dependencies are in place

- [X] T001 Verify Gradle dependencies include spring-boot-starter-web, spring-boot-starter-actuator in build.gradle.kts
- [X] T002 [P] Create package directories: src/main/java/com/example/demo/config/, src/main/java/com/example/demo/filter/, src/main/java/com/example/demo/ratelimit/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Create RateLimitCounter POJO with clientIp, requestCount, windowStartTime fields and increment/isExpired/reset methods in src/main/java/com/example/demo/ratelimit/RateLimitCounter.java
- [X] T004 Create RateLimitProperties configuration class with @ConfigurationProperties binding for ratelimit.requests-per-minute (default 60), ratelimit.window-duration-ms (default 60000), ratelimit.enabled (default true) in src/main/java/com/example/demo/config/RateLimitProperties.java
- [X] T005 Add @ConfigurationPropertiesScan or @EnableConfigurationProperties to DemoApplication.java in src/main/java/com/example/demo/DemoApplication.java
- [X] T006 Add rate limit configuration properties to src/main/resources/application.properties with defaults

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Rate Limited Feedback (Priority: P1) MVP

**Goal**: API consumers receive clear feedback (HTTP 429 with Retry-After header) when rate limited

**Independent Test**: Make 61+ rapid requests to /api/* endpoint; verify requests 1-60 succeed and 61+ return HTTP 429 with Retry-After header and JSON body

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T007 [P] [US1] Create RateLimitServiceTest with tests for: increment count, check limit exceeded, window expiration reset, multiple IPs tracked separately in src/test/java/com/example/demo/ratelimit/RateLimitServiceTest.java
- [X] T008 [P] [US1] Create RateLimitFilterTest with tests for: request within limit passes through, request exceeding limit returns 429, Retry-After header present, JSON response body correct in src/test/java/com/example/demo/filter/RateLimitFilterTest.java
- [X] T009 [P] [US1] Create RateLimitIntegrationTest with @SpringBootTest for end-to-end rate limiting on /api/* endpoints in src/test/java/com/example/demo/integration/RateLimitIntegrationTest.java

### Implementation for User Story 1

- [X] T010 [US1] Create RateLimitResponse DTO with error, message, retryAfter fields for 429 response body in src/main/java/com/example/demo/ratelimit/RateLimitResponse.java
- [X] T011 [US1] Implement RateLimitService with ConcurrentHashMap storage, atomic compute() for increment, fixed window algorithm, client IP tracking in src/main/java/com/example/demo/ratelimit/RateLimitService.java
- [X] T012 [US1] Implement RateLimitFilter extending OncePerRequestFilter with doFilterInternal checking rate limit and returning 429 with Retry-After header in src/main/java/com/example/demo/filter/RateLimitFilter.java
- [X] T013 [US1] Add FilterRegistrationBean configuration for RateLimitFilter with /api/* URL pattern in src/main/java/com/example/demo/config/RateLimitFilterConfig.java
- [X] T014 [US1] Implement client IP extraction from X-Forwarded-For header with fallback to getRemoteAddr() in RateLimitFilter
- [X] T015 [US1] Create sample /api/test endpoint for manual testing in src/main/java/com/example/demo/controller/TestController.java
- [X] T016 [US1] Run User Story 1 tests and verify they pass

**Checkpoint**: Rate limiting works - requests exceeding limit get HTTP 429 with Retry-After header

---

## Phase 4: User Story 2 - Configurable Rate Limit (Priority: P2)

**Goal**: Operators can configure rate limit threshold via application.properties without code changes

**Independent Test**: Change ratelimit.requests-per-minute to 30, restart app, verify 31st request returns 429

### Tests for User Story 2

- [X] T017 [P] [US2] Add configuration tests to verify: custom limit applied, default 60 when not specified, validation rejects invalid values in src/test/java/com/example/demo/config/RateLimitPropertiesTest.java
- [X] T018 [P] [US2] Add integration test for configuration: start with limit=30, verify 31st request returns 429 in src/test/java/com/example/demo/integration/RateLimitConfigIntegrationTest.java

### Implementation for User Story 2

- [X] T019 [US2] Add JSR-380 validation annotations (@Min, @Max) to RateLimitProperties for requests-per-minute range 1-10000 in src/main/java/com/example/demo/config/RateLimitProperties.java
- [X] T020 [US2] Add spring-boot-starter-validation dependency to build.gradle.kts if not present
- [X] T021 [US2] Run User Story 2 tests and verify they pass

**Checkpoint**: Configuration is validated and applied correctly without code changes

---

## Phase 5: User Story 3 - Excluded Health Endpoints (Priority: P3)

**Goal**: Health and actuator endpoints are always accessible regardless of rate limit status

**Independent Test**: After being rate limited on /api/*, verify /actuator/health still returns 200

### Tests for User Story 3

- [X] T022 [P] [US3] Add exclusion tests to RateLimitFilterTest: verify /actuator/* and /health paths bypass rate limiting in src/test/java/com/example/demo/filter/RateLimitFilterTest.java
- [X] T023 [P] [US3] Add integration test for exclusion: rate limit client on /api/*, then verify /actuator/health succeeds in src/test/java/com/example/demo/integration/RateLimitExclusionIntegrationTest.java

### Implementation for User Story 3

- [X] T024 [US3] Override shouldNotFilter() in RateLimitFilter to exclude /actuator/** and /health paths in src/main/java/com/example/demo/filter/RateLimitFilter.java
- [X] T025 [US3] Run User Story 3 tests and verify they pass

**Checkpoint**: Actuator and health endpoints always accessible regardless of rate limit status

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T026 Implement ScheduledExecutorService cleanup task to remove expired rate limit entries every 60 seconds in src/main/java/com/example/demo/ratelimit/RateLimitService.java
- [X] T027 [P] Add logging for rate limit events (limit exceeded, window reset) in RateLimitFilter and RateLimitService
- [X] T028 Run full test suite: ./gradlew test
- [X] T029 Validate quickstart.md scenarios manually: build, rate limit test loop, 429 response check, actuator exclusion
- [X] T030 Run ./gradlew build to verify project compiles and all tests pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can proceed in parallel if staffed
  - Or sequentially in priority order (P1 -> P2 -> P3)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent of US1 (configuration already works)
- **User Story 3 (P3)**: Can start after US1 (needs filter to exist) - Adds exclusion logic to existing filter

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- POJOs/DTOs before services
- Services before filters
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

**Phase 1**:
- T001 and T002 can run in parallel

**Phase 2**:
- T003 and T004 can run in parallel (different files)

**Phase 3 (US1)**:
- T007, T008, T009 can all run in parallel (test files)
- After tests pass: T010 through T014 must be sequential (dependencies)

**Phase 4 (US2)**:
- T017 and T018 can run in parallel (test files)

**Phase 5 (US3)**:
- T022 and T023 can run in parallel (test files)

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Create RateLimitServiceTest in src/test/java/com/example/demo/ratelimit/RateLimitServiceTest.java"
Task: "Create RateLimitFilterTest in src/test/java/com/example/demo/filter/RateLimitFilterTest.java"
Task: "Create RateLimitIntegrationTest in src/test/java/com/example/demo/integration/RateLimitIntegrationTest.java"

# Then implement sequentially (dependencies exist):
Task: "Create RateLimitResponse DTO"
Task: "Implement RateLimitService"
Task: "Implement RateLimitFilter"
Task: "Add FilterRegistrationBean config"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test rate limiting works - 61st request returns 429
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational -> Foundation ready
2. Add User Story 1 -> Test independently -> Deploy/Demo (MVP!)
3. Add User Story 2 -> Test independently -> Deploy/Demo
4. Add User Story 3 -> Test independently -> Deploy/Demo
5. Each story adds value without breaking previous stories

### Task Summary by Story

| Phase | Story | Task Count | Parallel Tasks |
|-------|-------|------------|----------------|
| Phase 1 | Setup | 2 | 2 |
| Phase 2 | Foundational | 4 | 2 |
| Phase 3 | US1 - Rate Limited Feedback | 10 | 3 (tests) |
| Phase 4 | US2 - Configurable Rate Limit | 5 | 2 (tests) |
| Phase 5 | US3 - Excluded Health Endpoints | 4 | 2 (tests) |
| Phase 6 | Polish | 5 | 1 |
| **Total** | | **30** | **12** |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Key files: RateLimitFilter.java, RateLimitService.java, RateLimitProperties.java
