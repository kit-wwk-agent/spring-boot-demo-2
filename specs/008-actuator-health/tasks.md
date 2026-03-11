# Tasks: Actuator Health Endpoint

**Input**: Design documents from `/specs/008-actuator-health/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/health-api.md

**Tests**: Included based on test structure defined in plan.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Source code**: `src/main/java/com/example/demo/`
- **Configuration**: `src/main/resources/`
- **Tests**: `src/test/java/com/example/demo/`
- **Test configuration**: `src/test/resources/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify project dependencies and base structure

- [X] T001 Verify spring-boot-starter-actuator dependency exists in build.gradle
- [X] T002 [P] Create test base class in src/test/java/com/example/demo/health/ActuatorHealthTestBase.java
- [X] T003 [P] Create test configuration in src/test/resources/application-test.yml

---

## Phase 2: Foundational (Health Endpoint Configuration)

**Purpose**: Core health endpoint configuration that MUST be complete before ANY user story testing

**⚠️ CRITICAL**: No user story validation can begin until this phase is complete

- [X] T004 Configure health endpoint exposure in src/main/resources/application.yml with endpoints.web.exposure.include: health,info,metrics
- [X] T005 Configure health endpoint details visibility (show-details: when_authorized, show-components: when_authorized) in src/main/resources/application.yml
- [X] T006 Enable Kubernetes probes (management.endpoint.health.probes.enabled: true) in src/main/resources/application.yml
- [X] T007 Configure liveness health group (include: livenessState only) in src/main/resources/application.yml
- [X] T008 Configure readiness health group (include: readinessState, db, diskSpace) in src/main/resources/application.yml
- [X] T009 Configure health cache TTL (1000ms) for performance in src/main/resources/application.yml
- [X] T010 Configure disk space threshold (10485760 bytes = 10MB) in src/main/resources/application.yml
- [X] T011 [P] Configure database connection timeout (250ms) in src/main/resources/application.yml for fast failure detection
- [X] T012 Enable full health details (show-details: always, show-components: always) in src/test/resources/application-test.yml

**Checkpoint**: Health endpoint configuration complete - user story testing can now begin

---

## Phase 3: User Story 1 - Kubernetes Liveness Probe Configuration (Priority: P1) 🎯 MVP

**Goal**: DevOps engineers can hit /actuator/health to configure Kubernetes liveness probes

**Independent Test**: Send GET request to /actuator/health and verify response format (200 with {"status":"UP"} or 503 with {"status":"DOWN"}) and unauthenticated access

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before configuration is complete**

- [X] T013 [P] [US1] Create BasicHealthCheckTest in src/test/java/com/example/demo/health/BasicHealthCheckTest.java with test for 200 status and UP response
- [X] T014 [P] [US1] Add test for JSON response format {"status":"UP"} with Content-Type application/json in src/test/java/com/example/demo/health/BasicHealthCheckTest.java
- [X] T015 [P] [US1] Add test for unauthenticated access (no 401/403) in src/test/java/com/example/demo/health/BasicHealthCheckTest.java
- [X] T016 [P] [US1] Add test for /actuator/health/liveness endpoint returns 200 with UP in src/test/java/com/example/demo/health/BasicHealthCheckTest.java
- [X] T017 [P] [US1] Add test for /actuator/health/readiness endpoint returns 200 with UP in src/test/java/com/example/demo/health/BasicHealthCheckTest.java

### Validation for User Story 1

- [X] T018 [US1] Run BasicHealthCheckTest and verify all tests pass
- [X] T019 [US1] Manually verify curl http://localhost:8080/actuator/health returns {"status":"UP"}

**Checkpoint**: User Story 1 complete - basic health endpoint functional for Kubernetes probes

---

## Phase 4: User Story 2 - Monitoring System Integration (Priority: P2)

**Goal**: Monitoring systems receive structured health data that reflects actual database connectivity state

**Independent Test**: Verify /actuator/health returns DOWN when database is unreachable and UP when connectivity is restored

### Tests for User Story 2

- [X] T020 [P] [US2] Create DatabaseHealthCheckTest in src/test/java/com/example/demo/health/DatabaseHealthCheckTest.java with test for db component presence
- [X] T021 [P] [US2] Add test for database health indicator status UP when connected in src/test/java/com/example/demo/health/DatabaseHealthCheckTest.java
- [X] T022 [US2] Add test for overall status DOWN (503) when database is unreachable in src/test/java/com/example/demo/health/DatabaseHealthCheckTest.java

### Validation for User Story 2

- [X] T023 [US2] Run DatabaseHealthCheckTest and verify all tests pass

**Checkpoint**: User Story 2 complete - monitoring systems can detect database health changes

---

## Phase 5: User Story 3 - Load Balancer Health Check (Priority: P2)

**Goal**: Load balancers can check /actuator/health to route traffic only to healthy instances (disk space availability)

**Independent Test**: Verify disk space health indicator affects overall status

### Tests for User Story 3

- [X] T024 [P] [US3] Create DiskSpaceHealthCheckTest in src/test/java/com/example/demo/health/DiskSpaceHealthCheckTest.java with test for diskSpace component presence
- [X] T025 [P] [US3] Add test for disk space health indicator status UP when above threshold in src/test/java/com/example/demo/health/DiskSpaceHealthCheckTest.java
- [X] T026 [US3] Add test for disk space threshold configuration (10MB) in src/test/java/com/example/demo/health/DiskSpaceHealthCheckTest.java

### Validation for User Story 3

- [X] T027 [US3] Run DiskSpaceHealthCheckTest and verify all tests pass

**Checkpoint**: User Story 3 complete - load balancers can detect disk space health

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Integration testing and final validation

- [X] T028 [P] Create HealthEndpointIntegrationTest in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java
- [X] T029 [P] Add integration test for response time < 500ms in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java
- [X] T030 Add integration test for all health endpoints working together in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java
- [X] T031 Run all health tests with ./gradlew test --tests "*Health*"
- [X] T032 Run quickstart.md validation (manual curl tests from quickstart.md)
- [X] T033 Verify application starts cleanly with ./gradlew bootRun

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user story validation
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Tests database health indicator
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Tests disk space health indicator

### Within Each User Story

- Tests SHOULD be written first to define expected behavior
- Run tests to validate configuration
- Tests can run in parallel within a story (different test files or different test methods)

### Parallel Opportunities

- T002, T003 can run in parallel (Setup phase)
- T004-T012 are mostly sequential (configuring same file), but T011 can run in parallel if split into separate config
- T013-T017 can all run in parallel (same test class but different test methods)
- T020-T022 tests can run in parallel with T024-T026 (different test classes)
- T028, T029 can run in parallel (same test class setup)

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all US1 tests together (they test different aspects):
Task: "Create BasicHealthCheckTest with test for 200 status"
Task: "Add test for JSON response format"
Task: "Add test for unauthenticated access"
Task: "Add test for /actuator/health/liveness endpoint"
Task: "Add test for /actuator/health/readiness endpoint"
```

## Parallel Example: Cross-Story Testing

```bash
# Once Foundational is complete, all user story tests can be written in parallel:
Task: "Create BasicHealthCheckTest [US1]"
Task: "Create DatabaseHealthCheckTest [US2]"
Task: "Create DiskSpaceHealthCheckTest [US3]"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verify dependencies, create test base)
2. Complete Phase 2: Foundational (configure application.yml)
3. Complete Phase 3: User Story 1 (basic health check tests)
4. **STOP and VALIDATE**: Run tests, verify curl commands work
5. Deploy/demo if ready - basic Kubernetes liveness probe support complete

### Incremental Delivery

1. Complete Setup + Foundational → Health endpoint exposed
2. Add User Story 1 tests → Verify basic health check → Deploy (MVP!)
3. Add User Story 2 tests → Verify database health indicator → Deploy
4. Add User Story 3 tests → Verify disk space indicator → Deploy
5. Add integration tests → Full validation → Production ready

### Key Implementation Notes

- **This is a configuration-only feature** - no custom Java source code required
- All functionality comes from Spring Boot Actuator auto-configuration
- Primary implementation is YAML configuration in application.yml
- Tests validate the configuration works as expected
- No custom health indicators needed - built-in indicators meet all requirements

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently testable after Foundational phase
- This feature leverages Spring Boot Actuator - minimal custom code required
- Configuration changes in application.yml are the core implementation
- Test files match structure defined in plan.md
- Commit after each phase completion for clean history
