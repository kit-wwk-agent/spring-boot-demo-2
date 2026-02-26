# Tasks: Actuator Health Endpoint

**Input**: Design documents from `/specs/006-actuator-health/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Note**: This feature is primarily configuration-driven using Spring Boot Actuator's built-in health infrastructure. The core implementation is already complete in the codebase. These tasks focus on verification, validation, and any refinements needed.

**Tests**: Integration tests already exist in `HealthEndpointIntegrationTest.java`. No additional test tasks required.

**Organization**: Tasks are grouped by user story to enable independent verification and validation.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Verification)

**Purpose**: Verify project dependencies and basic structure are correct

- [X] T001 Verify spring-boot-starter-actuator dependency in build.gradle.kts
- [X] T002 [P] Verify spring-boot-starter-data-jpa dependency in build.gradle.kts
- [X] T003 [P] Verify H2 and PostgreSQL runtime dependencies in build.gradle.kts

---

## Phase 2: Foundational (Configuration Verification)

**Purpose**: Verify core actuator configuration is correct and complete

**Status**: Configuration already exists in `src/main/resources/application.yml`

- [X] T004 Verify actuator endpoints exposure configuration (health, info, metrics) in src/main/resources/application.yml
- [X] T005 [P] Verify health endpoint caching configuration (1000ms TTL) in src/main/resources/application.yml
- [X] T006 [P] Verify HikariCP connection timeout settings (250ms) in src/main/resources/application.yml
- [X] T007 Verify test profile configuration enables full health details in src/test/resources/application-test.yml

**Checkpoint**: Foundation verified - user story validation can now begin in parallel

---

## Phase 3: User Story 1 - Basic Health Check (Priority: P1) :dart: MVP

**Goal**: DevOps engineer can hit health endpoint for Kubernetes liveness and readiness probes

**Independent Test**: GET `/actuator/health` returns 200 OK with `{"status": "UP"}` JSON response

### Verification for User Story 1

- [X] T008 [US1] Verify health endpoint path configured at /actuator/health in src/main/resources/application.yml
- [X] T009 [P] [US1] Verify Kubernetes probes enabled (liveness, readiness) in src/main/resources/application.yml
- [X] T010 [P] [US1] Verify liveness probe health group configuration in src/main/resources/application.yml
- [X] T011 [P] [US1] Verify readiness probe health group configuration in src/main/resources/application.yml
- [X] T012 [US1] Run basic health check tests in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java

**Checkpoint**: User Story 1 (Basic Health Check) verified - MVP functionality confirmed

---

## Phase 4: User Story 2 - Database Health Monitoring (Priority: P2)

**Goal**: Monitoring system can check database connectivity via health endpoint

**Independent Test**: When database is healthy, `/actuator/health` includes `db` component with `status: UP`

### Verification for User Story 2

- [X] T013 [US2] Verify DataSourceHealthIndicator is auto-configured via Spring Data JPA dependency
- [X] T014 [P] [US2] Verify database health included in readiness probe group in src/main/resources/application.yml
- [X] T015 [US2] Run database health indicator tests in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java

**Checkpoint**: User Story 2 (Database Health) verified - database connectivity monitoring confirmed

---

## Phase 5: User Story 3 - Disk Space Monitoring (Priority: P3)

**Goal**: Operations engineer can monitor disk space via health endpoint

**Independent Test**: `/actuator/health` includes `diskSpace` component with threshold monitoring

### Verification for User Story 3

- [X] T016 [US3] Verify disk space health indicator enabled in src/main/resources/application.yml
- [X] T017 [P] [US3] Verify disk space threshold configured (10485760 bytes / 10MB) in src/main/resources/application.yml
- [X] T018 [P] [US3] Verify disk space monitoring path configured (/) in src/main/resources/application.yml
- [X] T019 [US3] Run disk space health indicator tests in src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java

**Checkpoint**: User Story 3 (Disk Space) verified - disk space monitoring confirmed

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [X] T020 Run all integration tests to verify complete health endpoint functionality
- [X] T021 [P] Verify unauthenticated access tests pass (FR-006 compliance)
- [X] T022 [P] Verify performance tests pass (< 500ms response time per FR-007)
- [X] T023 Validate quickstart.md scenarios work correctly by manual testing
- [X] T024 Verify all functional requirements (FR-001 through FR-007) are satisfied

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 -> P2 -> P3)
- **Polish (Final Phase)**: Depends on all user stories being verified

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent of US1
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of US1/US2

### Within Each User Story

- Configuration verification before test execution
- All configuration tasks marked [P] can run in parallel
- Test execution after configuration verification

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user story verifications can start in parallel
- All configuration verification tasks within a story marked [P] can run in parallel
- Different user stories can be verified in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all configuration verifications for User Story 1 together:
Task: "Verify Kubernetes probes enabled (liveness, readiness) in src/main/resources/application.yml"
Task: "Verify liveness probe health group configuration in src/main/resources/application.yml"
Task: "Verify readiness probe health group configuration in src/main/resources/application.yml"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup verification
2. Complete Phase 2: Foundational configuration verification
3. Complete Phase 3: User Story 1 verification
4. **STOP and VALIDATE**: Run integration tests for basic health check
5. Deploy/demo if ready - basic Kubernetes probe support functional

### Incremental Verification

1. Verify Setup + Foundational -> Configuration confirmed
2. Verify User Story 1 -> Run tests -> Basic health check works (MVP!)
3. Verify User Story 2 -> Run tests -> Database monitoring works
4. Verify User Story 3 -> Run tests -> Disk space monitoring works
5. Each story adds verified value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational verification together
2. Once Foundational is verified:
   - Developer A: User Story 1 verification
   - Developer B: User Story 2 verification
   - Developer C: User Story 3 verification
3. Stories verified and tested independently

---

## Configuration Files Reference

| File | Purpose | Status |
|------|---------|--------|
| `build.gradle.kts` | Project dependencies | Complete |
| `src/main/resources/application.yml` | Main actuator configuration | Complete |
| `src/main/resources/application-dev.yml` | Dev profile | Inherits from main |
| `src/main/resources/application-prod.yml` | Production profile | Inherits from main |
| `src/test/resources/application-test.yml` | Test profile with full details | Complete |

## Test Files Reference

| File | Coverage |
|------|----------|
| `src/test/java/com/example/demo/integration/HealthEndpointIntegrationTest.java` | All user stories covered |

---

## Notes

- [P] tasks = different files/concerns, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently verifiable
- Commit after each verification phase
- Stop at any checkpoint to validate story independently
- This feature is configuration-driven - no custom code required
- All health indicators use Spring Boot Actuator built-in implementations
