# Tasks: Actuator Health Endpoint

**Input**: Design documents from `/specs/005-actuator-health/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/health-api.md, quickstart.md

**Tests**: Included - research.md explicitly lists "Integration tests" as required work.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/`, `src/test/java/` at repository root
- Paths follow existing Spring Boot project structure from plan.md

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify existing configuration and establish baseline

- [ ] T001 Verify actuator dependency exists in build.gradle.kts
- [ ] T002 [P] Review existing health configuration in src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core configuration that MUST be complete before ANY user story can be verified

**Critical**: No user story validation can begin until this phase is complete

- [ ] T003 Create production profile configuration in src/main/resources/application-prod.yml with PostgreSQL datasource and health settings
- [ ] T004 [P] Update src/main/resources/application-dev.yml to show full health details (show-details: always)
- [ ] T005 [P] Update src/main/resources/application-test.yml to show full health details for test assertions

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Basic Health Check for Kubernetes Probes (Priority: P1) - MVP

**Goal**: Enable Kubernetes liveness and readiness probes with /actuator/health endpoint returning HTTP 200/503 with UP/DOWN status

**Independent Test**: Send GET request to /actuator/health, verify HTTP 200 with `{"status": "UP"}` when healthy, HTTP 503 with `{"status": "DOWN"}` when unhealthy

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T006 [P] [US1] Create health endpoint integration test class in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java
- [ ] T007 [P] [US1] Add test for healthy application returns HTTP 200 with status UP in HealthEndpointIntegrationTest.java
- [ ] T008 [P] [US1] Add test for response time under 500ms in HealthEndpointIntegrationTest.java

### Implementation for User Story 1

- [ ] T009 [US1] Verify health endpoint enabled and exposed at /actuator/health in src/main/resources/application.yml
- [ ] T010 [US1] Configure liveness probe group (livenessState only) in src/main/resources/application.yml
- [ ] T011 [US1] Configure readiness probe group (readinessState, db, diskSpace) in src/main/resources/application.yml
- [ ] T012 [US1] Enable probe endpoints (/actuator/health/liveness, /actuator/health/readiness) in src/main/resources/application.yml
- [ ] T013 [US1] Configure response caching (1000ms TTL) for performance in src/main/resources/application.yml

**Checkpoint**: User Story 1 should be fully functional - basic health check works with Kubernetes probes

---

## Phase 4: User Story 2 - Structured Health Data for Monitoring Systems (Priority: P2)

**Goal**: Return component-level health details (db, diskSpace) in structured JSON format for monitoring systems

**Independent Test**: Send GET request to /actuator/health, verify response includes components object with db and diskSpace status information

### Tests for User Story 2

- [ ] T014 [P] [US2] Add test for database health indicator included in response in HealthEndpointIntegrationTest.java
- [ ] T015 [P] [US2] Add test for disk space health indicator included in response in HealthEndpointIntegrationTest.java
- [ ] T016 [P] [US2] Add test for overall status DOWN when database is unhealthy in HealthEndpointIntegrationTest.java

### Implementation for User Story 2

- [ ] T017 [US2] Configure database health indicator enabled in src/main/resources/application.yml
- [ ] T018 [US2] Configure disk space health indicator with 10MB threshold in src/main/resources/application.yml
- [ ] T019 [US2] Configure HikariCP connection timeout (250ms) for health check performance in src/main/resources/application.yml
- [ ] T020 [US2] Configure HikariCP validation timeout (250ms) for health check performance in src/main/resources/application.yml
- [ ] T021 [US2] Configure show-details: when_authorized for production, show-details: always for dev profile in src/main/resources/application.yml

**Checkpoint**: User Story 2 should be fully functional - monitoring systems can see component-level health details

---

## Phase 5: User Story 3 - Unauthenticated Access for Infrastructure Tools (Priority: P3)

**Goal**: Ensure health endpoints are accessible without authentication for load balancers and Kubernetes probes

**Independent Test**: Send GET request to /actuator/health without any authentication headers, verify successful response

### Tests for User Story 3

- [ ] T022 [P] [US3] Add test for health endpoint accessible without authentication in HealthEndpointIntegrationTest.java
- [ ] T023 [P] [US3] Add test for liveness endpoint accessible without authentication in HealthEndpointIntegrationTest.java
- [ ] T024 [P] [US3] Add test for readiness endpoint accessible without authentication in HealthEndpointIntegrationTest.java

### Implementation for User Story 3

- [ ] T025 [US3] Verify no Spring Security configuration blocks health endpoints (inspect existing security config if present)
- [ ] T026 [US3] Document health endpoint security posture in src/main/resources/application.yml comments

**Checkpoint**: User Story 3 should be fully functional - infrastructure tools can access health endpoints without credentials

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [ ] T027 Run all integration tests to verify complete health endpoint behavior
- [ ] T028 [P] Validate against quickstart.md verification checklist manually
- [ ] T029 [P] Add Kubernetes probe configuration example to quickstart.md if not already present
- [ ] T030 Run application and manually verify all three endpoints respond correctly

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 - P2 - P3)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Builds on US1 configuration but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of other stories

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Configuration tasks can often run in parallel (different config sections)
- Verify tests pass after implementation
- Story complete before moving to next priority

### Parallel Opportunities

- All tasks marked [P] can run in parallel (within their phase)
- Once Foundational phase completes, all user stories can start in parallel
- T006, T007, T008 (US1 tests) can run in parallel
- T014, T015, T016 (US2 tests) can run in parallel
- T022, T023, T024 (US3 tests) can run in parallel
- T027, T028, T029 (polish tasks) can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Create health endpoint integration test class in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java"
Task: "Add test for healthy application returns HTTP 200 with status UP"
Task: "Add test for response time under 500ms"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready - basic Kubernetes health probes work

### Incremental Delivery

1. Complete Setup + Foundational - Foundation ready
2. Add User Story 1 - Test independently - Deploy/Demo (MVP!)
3. Add User Story 2 - Test independently - Deploy/Demo (monitoring integration)
4. Add User Story 3 - Test independently - Deploy/Demo (complete feature)
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (core health endpoint)
   - Developer B: User Story 2 (component details)
   - Developer C: User Story 3 (security verification)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files or config sections, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Most configuration already exists - tasks focus on verification and ensuring correctness
- This feature leverages Spring Boot Actuator's built-in health infrastructure
- No custom health indicators needed - using DataSourceHealthIndicator and DiskSpaceHealthIndicator
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
