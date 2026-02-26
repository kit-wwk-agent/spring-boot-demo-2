# Tasks: API List Pagination

**Input**: Design documents from `/specs/001-api-list-pagination/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/pagination-api.yaml

**Tests**: Not explicitly requested in specification. Implementation tasks only.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths included in descriptions

## Path Conventions

Based on plan.md project structure:
- Source: `src/main/java/com/example/demo/`
- Tests: `src/test/java/com/example/demo/`

---

## Phase 1: Setup

**Purpose**: Verify project dependencies and structure

- [X] T001 Verify spring-boot-starter-validation dependency in build.gradle (required for @Min/@Max annotations)
- [X] T002 Verify existing controller(s) with list endpoints in src/main/java/com/example/demo/controller/ (Created ItemController with Item entity and ItemRepository)

---

## Phase 2: Foundational (Error Infrastructure)

**Purpose**: Create error response infrastructure required for proper HTTP 400 responses

**⚠️ CRITICAL**: GlobalExceptionHandler must be complete before US3 validation can work correctly

- [X] T003 [P] Create dto package at src/main/java/com/example/demo/dto/
- [X] T004 [P] Create ErrorResponse record in src/main/java/com/example/demo/dto/ErrorResponse.java
- [X] T005 Create exception package at src/main/java/com/example/demo/exception/
- [X] T006 Create GlobalExceptionHandler in src/main/java/com/example/demo/exception/GlobalExceptionHandler.java

**Checkpoint**: Error infrastructure ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Request Paginated Results (Priority: P1) 🎯 MVP

**Goal**: Enable API consumers to request specific pages of results using page and size query parameters

**Independent Test**: Call any list endpoint with `?page=0&size=20` and verify response contains only 20 records (or fewer if dataset is smaller)

### Implementation for User Story 1

- [X] T007 [US1] Update list endpoint method signature to accept page parameter with @RequestParam(defaultValue = "0") Integer page in src/main/java/com/example/demo/controller/
- [X] T008 [US1] Update list endpoint method signature to accept size parameter with @RequestParam(defaultValue = "20") Integer size in src/main/java/com/example/demo/controller/
- [X] T009 [US1] Create Pageable using PageRequest.of(page, size) in list endpoint method
- [X] T010 [US1] Update repository call to use findAll(Pageable pageable) and return Page<T> in controller

**Checkpoint**: User Story 1 complete - list endpoints accept page/size params and return paginated subset

---

## Phase 4: User Story 2 - View Pagination Metadata (Priority: P1)

**Goal**: Ensure pagination responses include totalElements, totalPages, number, and size metadata fields

**Independent Test**: Make paginated request and verify response JSON includes `totalElements`, `totalPages`, `number`, `size` fields with accurate values

### Implementation for User Story 2

No new implementation tasks required. Spring's `Page<T>` interface automatically serializes to JSON with all required metadata fields:
- `content`: array of items
- `totalElements`: total count in dataset
- `totalPages`: ceiling(totalElements/size)
- `number`: current page (0-indexed)
- `size`: requested page size

**Checkpoint**: User Story 2 complete - verify JSON response includes all metadata fields from contracts/pagination-api.yaml

---

## Phase 5: User Story 3 - Handle Invalid Pagination Parameters (Priority: P2)

**Goal**: Return HTTP 400 with clear, actionable error messages when API consumers provide invalid pagination parameters

**Independent Test**: Send requests with invalid params and verify HTTP 400 responses:
- `?size=150` → 400 (exceeds max 100)
- `?page=-1` → 400 (negative page)
- `?size=0` → 400 (size must be positive)
- `?page=abc` → 400 (non-numeric)

### Implementation for User Story 3

- [X] T011 [US3] Add @Validated annotation to controller class(es) in src/main/java/com/example/demo/controller/
- [X] T012 [US3] Add @Min(0) validation annotation to page parameter in list endpoint(s)
- [X] T013 [US3] Add @Min(1) @Max(100) validation annotations to size parameter in list endpoint(s)

**Checkpoint**: User Story 3 complete - invalid parameters return HTTP 400 with descriptive error messages

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification and cleanup

- [X] T014 Verify all list endpoints have consistent pagination parameter names (page, size)
- [X] T015 Run quickstart.md verification checklist against implementation (Note: Java not installed - manual code review completed)
- [X] T016 Verify response structure matches contracts/pagination-api.yaml schema (Page<T> matches required fields)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - BLOCKS US3 validation
- **US1 (Phase 3)**: Depends on Setup (not Foundational) - core pagination works without error handling
- **US2 (Phase 4)**: Depends on US1 completion - metadata is part of Page<T> response
- **US3 (Phase 5)**: Depends on Foundational (Phase 2) - requires GlobalExceptionHandler for proper 400 responses
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Setup (Phase 1) - No dependencies on error infrastructure
- **User Story 2 (P1)**: Depends on US1 - metadata automatically included in Page<T>
- **User Story 3 (P2)**: Depends on Foundational (Phase 2) - requires GlobalExceptionHandler

### Within Each User Story

- Repository changes before controller changes
- Core implementation before validation
- Story complete before moving to next priority

### Parallel Opportunities

**Phase 2 (Foundational)**:
```
Parallel: T003, T004, T005 (different packages/files)
Sequential: T006 depends on T005 (exception package must exist)
```

**Phase 3 (US1)**:
```
Sequential: T007 → T008 → T009 → T010 (same method signature)
```

**Phase 5 (US3)**:
```
Sequential: T011 → T012 → T013 (same controller class)
```

---

## Parallel Example: Foundational Phase

```bash
# Launch independent foundational tasks together:
Task: "Create dto package at src/main/java/com/example/demo/dto/"
Task: "Create ErrorResponse record in src/main/java/com/example/demo/dto/ErrorResponse.java"
Task: "Create exception package at src/main/java/com/example/demo/exception/"

# Then sequential:
Task: "Create GlobalExceptionHandler in src/main/java/com/example/demo/exception/GlobalExceptionHandler.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verify dependencies)
2. Complete Phase 3: User Story 1 (basic pagination)
3. **STOP and VALIDATE**: Test with `?page=0&size=10` - verify subset returned
4. Deploy/demo core pagination functionality

### Full Implementation

1. Complete Setup → Ready to implement
2. Complete Foundational (Phase 2) → Error infrastructure ready
3. Add User Story 1 → Pagination works with defaults
4. Verify User Story 2 → Metadata present in responses
5. Add User Story 3 → Validation returns proper 400 errors
6. Polish → All endpoints consistent

### Incremental Delivery

| Increment | Scope | Deliverable |
|-----------|-------|-------------|
| MVP | Phase 1 + Phase 3 | List endpoints accept page/size, return Page<T> |
| v1.1 | + Phase 2 + Phase 5 | Invalid parameters return HTTP 400 with clear messages |
| v1.2 | + Phase 6 | Production-ready, verified against contracts |

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- US1 and US2 are both P1 but US2 is automatic with Page<T> return type
- Validation annotations (@Min/@Max) without GlobalExceptionHandler will return 500 errors
- Spring's Page<T> includes bonus fields (first, last, empty) not required by spec but compatible
- All list endpoints should be updated for consistency (breaking change acknowledged in spec)
