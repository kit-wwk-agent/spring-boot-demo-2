# Tasks: Application Health Endpoint

**Input**: Design documents from `/specs/001-actuator-health/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/health-api.yaml

**Tests**: Not explicitly requested in the feature specification.

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths included in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize Spring Boot project with required dependencies

- [ ] T001 Create Gradle build configuration in build.gradle.kts with spring-boot-starter-actuator, spring-boot-starter-data-jpa, spring-boot-starter-web, h2, postgresql dependencies
- [ ] T002 [P] Create Gradle settings file in settings.gradle.kts with project name
- [ ] T003 [P] Create Spring Boot main application class in src/main/java/com/example/demo/DemoApplication.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core configuration that enables all health endpoint functionality

**⚠️ CRITICAL**: All user stories depend on this phase - it configures Spring Boot Actuator

- [ ] T004 Create main application configuration in src/main/resources/application.yml with Actuator endpoints exposure, health endpoint settings, probes configuration, health groups (liveness/readiness), disk space threshold, and HikariCP timeouts per research.md
- [ ] T005 [P] Create development profile configuration in src/main/resources/application-dev.yml with H2 datasource settings and show-details: always for local debugging
- [ ] T006 [P] Create test profile configuration in src/test/resources/application-test.yml with embedded H2 database for testing

**Checkpoint**: Foundation ready - Actuator health endpoints will be functional after application starts

---

## Phase 3: User Story 1 - Kubernetes Liveness/Readiness Probes (Priority: P1) 🎯 MVP

**Goal**: Enable Kubernetes to detect unhealthy pods via liveness/readiness probes and automatically restart or remove them from rotation

**Independent Test**: Start application, call GET /actuator/health/liveness and /actuator/health/readiness, verify 200 response with {"status":"UP"}. Stop database, verify readiness returns 503 while liveness remains 200.

### Implementation for User Story 1

- [ ] T007 [US1] Verify liveness probe configuration in src/main/resources/application.yml includes only livenessState indicator (no external dependencies) per Kubernetes best practices
- [ ] T008 [US1] Verify readiness probe configuration in src/main/resources/application.yml includes readinessState, db, and diskSpace indicators per contracts/health-api.yaml
- [ ] T009 [US1] Create Actuator security configuration in src/main/java/com/example/demo/config/ActuatorConfig.java to permit unauthenticated access to health endpoints (FR-006)
- [ ] T010 [US1] Validate health endpoint response time meets 500ms SLA (FR-008) by testing with curl and verifying HikariCP timeout settings (250ms connection-timeout, 250ms validation-timeout)

**Checkpoint**: Kubernetes liveness/readiness probes fully functional. Test scenarios from quickstart.md:
- `curl http://localhost:8080/actuator/health/liveness` returns 200
- `curl http://localhost:8080/actuator/health/readiness` returns 200/503 based on dependencies

---

## Phase 4: User Story 2 - Monitoring System Integration (Priority: P2)

**Goal**: Provide structured health data for monitoring systems to parse and alert on

**Independent Test**: Call GET /actuator/health, verify JSON response contains parseable status field. Simulate DB failure, verify status changes to DOWN with appropriate component details.

### Implementation for User Story 2

- [ ] T011 [US2] Verify application.yml configures show-details: when_authorized and show-components: when_authorized for structured health data visibility (FR-007)
- [ ] T012 [US2] Verify health response includes db and diskSpace components with details per data-model.md HealthComponent schema

**Checkpoint**: Monitoring systems can poll /actuator/health and receive structured JSON with component-level status information

---

## Phase 5: User Story 3 - Load Balancer Health Checks (Priority: P2)

**Goal**: Enable load balancers to route traffic only to healthy application instances

**Independent Test**: Call GET /actuator/health, verify HTTP 200 when healthy and HTTP 503 when any dependency fails. Load balancer can use this response code for routing decisions.

### Implementation for User Story 3

- [ ] T013 [US3] Verify /actuator/health returns HTTP 200 with {"status":"UP"} when all dependencies healthy (FR-002)
- [ ] T014 [US3] Verify /actuator/health returns HTTP 503 with {"status":"DOWN"} when any dependency unhealthy (FR-003)

**Checkpoint**: Load balancers can use the health endpoint for traffic routing - no false positives or negatives (SC-004)

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and verification across all user stories

- [ ] T015 Run full quickstart.md validation: start application, test all health endpoints, simulate database failure, verify response times
- [ ] T016 Verify OpenAPI contract compliance: responses match contracts/health-api.yaml schema for all endpoints (/actuator/health, /actuator/health/liveness, /actuator/health/readiness)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001-T003)
- **User Story 1 (Phase 3)**: Depends on Foundational (T004-T006)
- **User Story 2 (Phase 4)**: Depends on Foundational, can run parallel with US1
- **User Story 3 (Phase 5)**: Depends on Foundational, can run parallel with US1/US2
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Core Kubernetes probe functionality - independent, MVP
- **User Story 2 (P2)**: Structured data for monitoring - shares configuration with US1
- **User Story 3 (P2)**: Load balancer routing - uses same endpoint as US1

### Within Each Phase

```
Setup: T001 → T002 [P], T003 [P]
Foundational: T004 → T005 [P], T006 [P]
US1: T007 → T008 → T009 → T010
US2: T011 → T012
US3: T013 → T014
Polish: T015 → T016
```

### Parallel Opportunities

- T002 and T003 can run in parallel (Setup phase)
- T005 and T006 can run in parallel (Foundational phase)
- User Stories 1, 2, and 3 can be worked on in parallel after Foundational phase
- All verification tasks within US2 and US3 are configuration validations

---

## Parallel Example: Setup Phase

```bash
# After T001 completes, launch T002 and T003 together:
Task: "Create Gradle settings file in settings.gradle.kts"
Task: "Create Spring Boot main application class in src/main/java/com/example/demo/DemoApplication.java"
```

## Parallel Example: Foundational Phase

```bash
# After T004 completes, launch T005 and T006 together:
Task: "Create development profile configuration in src/main/resources/application-dev.yml"
Task: "Create test profile configuration in src/test/resources/application-test.yml"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T006)
3. Complete Phase 3: User Story 1 (T007-T010)
4. **STOP and VALIDATE**: Test Kubernetes probes per quickstart.md
5. Deploy/demo - Kubernetes can now manage pod health

### Incremental Delivery

1. Setup + Foundational → Spring Boot app with Actuator ready
2. Add User Story 1 → Kubernetes probes functional (MVP!)
3. Add User Story 2 → Monitoring systems can parse health data
4. Add User Story 3 → Load balancers verified working
5. Each story validates without breaking previous stories

### Key Implementation Notes

- **This feature is primarily configuration-based**: Spring Boot Actuator provides all health indicator functionality out-of-the-box
- **No custom health indicators needed**: DataSourceHealthIndicator and DiskSpaceHealthIndicator are auto-configured
- **Most tasks are verification**: Ensuring configuration matches requirements from research.md and contracts/
- **Single configuration file**: application.yml contains most health endpoint settings

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- This feature leverages Spring Boot Actuator - minimal custom code required
- Configuration is the primary deliverable (application.yml, ActuatorConfig.java)
- Verification tasks ensure contract compliance with health-api.yaml
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
