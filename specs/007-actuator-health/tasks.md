# Tasks: Actuator Health Endpoint

**Input**: Design documents from `/specs/007-actuator-health/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Key Finding**: Per research.md, Spring Boot Actuator is already fully configured. This feature focuses on verification testing to confirm all specification requirements are met.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Verification Infrastructure)

**Purpose**: Verify existing actuator configuration and prepare test infrastructure

- [ ] T001 Verify actuator health endpoint configuration in src/main/resources/application.yml
- [ ] T002 Verify database health indicator in readiness group in src/main/resources/application.yml
- [ ] T003 [P] Verify disk space health indicator configuration (10MB threshold) in src/main/resources/application.yml
- [ ] T004 [P] Create health test package directory at src/test/java/com/example/demo/health/

---

## Phase 2: Foundational (Test Infrastructure)

**Purpose**: Set up base test class and utilities needed by all health endpoint tests

**CRITICAL**: No user story tests can begin until this phase is complete

- [ ] T005 Create base test configuration with MockMvc setup in src/test/java/com/example/demo/health/ActuatorHealthTestBase.java
- [ ] T006 Configure test profile with H2 database in src/test/resources/application-test.properties (if not exists)

**Checkpoint**: Foundation ready - user story test implementation can now begin in parallel

---

## Phase 3: User Story 1 - Basic Health Check for Load Balancers (Priority: P1) MVP

**Goal**: Verify health endpoint returns correct HTTP 200 with `{"status":"UP"}` for load balancer integration

**Independent Test**: Send GET to /actuator/health, verify HTTP 200 and JSON body format

### Tests for User Story 1

- [ ] T007 [P] [US1] Test health endpoint returns HTTP 200 when healthy in src/test/java/com/example/demo/health/BasicHealthCheckTest.java
- [ ] T008 [P] [US1] Test health endpoint returns JSON body `{"status":"UP"}` when healthy in src/test/java/com/example/demo/health/BasicHealthCheckTest.java
- [ ] T009 [P] [US1] Test health endpoint accessible without authentication in src/test/java/com/example/demo/health/BasicHealthCheckTest.java
- [ ] T010 [US1] Test health endpoint response time under 500ms in src/test/java/com/example/demo/health/BasicHealthCheckTest.java
- [ ] T011 [US1] Test health endpoint returns Content-Type application/json in src/test/java/com/example/demo/health/BasicHealthCheckTest.java

**Checkpoint**: User Story 1 complete - basic load balancer health check verified

---

## Phase 4: User Story 2 - Database Connectivity Monitoring (Priority: P1)

**Goal**: Verify health endpoint correctly reports database connectivity status

**Independent Test**: Simulate database unavailability and verify HTTP 503 with `{"status":"DOWN"}`

### Tests for User Story 2

- [ ] T012 [P] [US2] Test health returns HTTP 503 with DOWN status when database unavailable in src/test/java/com/example/demo/health/DatabaseHealthCheckTest.java
- [ ] T013 [P] [US2] Test health returns HTTP 200 with UP status when database recovers in src/test/java/com/example/demo/health/DatabaseHealthCheckTest.java
- [ ] T014 [US2] Test readiness probe includes database status at /actuator/health/readiness in src/test/java/com/example/demo/health/DatabaseHealthCheckTest.java

**Checkpoint**: User Story 2 complete - database connectivity monitoring verified

---

## Phase 5: User Story 3 - Disk Space Monitoring (Priority: P2)

**Goal**: Verify health endpoint correctly reports disk space status

**Independent Test**: Verify disk space indicator contributes to overall health status

### Tests for User Story 3

- [ ] T015 [P] [US3] Test disk space indicator included in health response (with show-details=always) in src/test/java/com/example/demo/health/DiskSpaceHealthCheckTest.java
- [ ] T016 [US3] Test readiness probe includes disk space status at /actuator/health/readiness in src/test/java/com/example/demo/health/DiskSpaceHealthCheckTest.java
- [ ] T017 [US3] Verify disk space threshold configuration (10MB) is applied in src/test/java/com/example/demo/health/DiskSpaceHealthCheckTest.java

**Checkpoint**: User Story 3 complete - disk space monitoring verified

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [ ] T018 [P] Run all health endpoint tests and verify pass in src/test/java/com/example/demo/health/
- [ ] T019 [P] Verify liveness probe endpoint at /actuator/health/liveness returns expected response
- [ ] T020 Run quickstart.md validation - execute manual test commands from specs/007-actuator-health/quickstart.md
- [ ] T021 Verify all acceptance scenarios from spec.md pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - verify existing configuration
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user story tests
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 -> P1 -> P2)
- **Polish (Final Phase)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Independent of US1
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Independent of US1/US2

### Within Each User Story

- Test setup before test execution
- Each test file is independently testable
- Tests verify existing Actuator behavior (no implementation code needed)

### Parallel Opportunities

- T003 and T004 can run in parallel (Setup phase)
- T007, T008, T009 can run in parallel (US1 tests - same file but independent test methods)
- T012, T013 can run in parallel (US2 tests)
- T018, T019 can run in parallel (Polish phase)
- Different user stories (US1, US2, US3) can be worked on in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all independent tests for User Story 1 together:
Task: "Test health endpoint returns HTTP 200 when healthy"
Task: "Test health endpoint returns JSON body with status UP"
Task: "Test health endpoint accessible without authentication"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup verification
2. Complete Phase 2: Foundational test infrastructure
3. Complete Phase 3: User Story 1 tests
4. **STOP and VALIDATE**: Run US1 tests - verify basic health check works
5. Deploy/demo if MVP ready

### Incremental Delivery

1. Complete Setup + Foundational -> Test infrastructure ready
2. Add User Story 1 tests -> Run tests -> Verify (MVP!)
3. Add User Story 2 tests -> Run tests -> Verify database monitoring
4. Add User Story 3 tests -> Run tests -> Verify disk space monitoring
5. Each story's tests verify independent acceptance criteria

### Key Implementation Note

Per research.md findings: **No new code is required for core Actuator functionality**. The existing `application.yml` configuration already provides:
- Health endpoint at `/actuator/health`
- Database health indicator (DataSourceHealthIndicator)
- Disk space health indicator (DiskSpaceHealthIndicator)
- Correct HTTP status codes (200 UP, 503 DOWN)
- No authentication requirement

This feature is primarily about **test verification** to confirm specification compliance.

---

## Notes

- [P] tasks = different files or independent test methods, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story's tests are independently runnable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts
