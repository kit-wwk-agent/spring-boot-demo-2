# Research: API List Pagination

**Feature**: API List Pagination
**Date**: 2026-02-26
**Status**: Complete

## Executive Summary

This research validates that Spring Data JPA's built-in pagination support (`Pageable`, `Page<T>`) is ideal for implementing the spec requirements. Key decisions:

1. Use Spring Data `Pageable` with application-level configuration for max size
2. Implement global exception handler for validation errors
3. Use `Page<T>` (not `Slice<T>`) to satisfy metadata requirements
4. Database-level pagination ensures performance for 100k+ records

---

## Decision 1: Pagination Mechanism

**Decision**: Use Spring Data JPA `Pageable` and `Page<T>` interfaces

**Rationale**:
- Native integration with Spring Boot 3.4.x
- `Page<T>` response structure exactly matches spec requirements:
  - `content`: actual items array
  - `totalElements`: total count
  - `totalPages`: ceiling(totalElements/size)
  - `number`: current page (zero-indexed)
  - `size`: page size
- Database-level LIMIT/OFFSET prevents loading full dataset into memory
- Zero additional dependencies (already in spring-boot-starter-data-jpa)

**Alternatives Rejected**:
- **Cursor-based pagination**: Better for deep pagination (>5000 pages), but spec uses simple page/size parameters. OFFSET-based is sufficient for target scale.
- **Slice<T>**: Eliminates count query but doesn't provide `totalElements`/`totalPages` required by spec.
- **Custom pagination framework**: Unnecessary complexity when Spring provides complete solution.

---

## Decision 2: Parameter Validation Approach

**Decision**: Controller-level validation with `@Validated` + `@Min`/`@Max` annotations + `@ControllerAdvice` for error handling

**Rationale**:
- Spring's `spring.data.web.pageable.max-page-size` auto-caps silently (doesn't return 400 error)
- Spec requires HTTP 400 with descriptive error message for invalid parameters
- Jakarta validation (`@Min`, `@Max`) + `@ControllerAdvice` provides consistent error responses

**Implementation Pattern**:
```java
@GetMapping
public Page<T> list(
    @RequestParam(defaultValue = "0") @Min(0) Integer page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
    // ...
}
```

**Error Response Format**:
```json
{
  "error": "Validation failed",
  "message": "size: must be less than or equal to 100",
  "status": 400,
  "timestamp": "2026-02-26T10:30:00"
}
```

**Alternatives Rejected**:
- **Spring auto-capping**: Silently adjusts size without error (violates SC-005)
- **Manual validation in controller**: Code duplication across endpoints
- **Custom filter/interceptor**: Overkill when validation annotations work

---

## Decision 3: Default Values

**Decision**: Default page=0, size=20 via `@RequestParam(defaultValue = ...)`

**Rationale**:
- Matches spec FR-001 (page default 0) and FR-002 (size default 20)
- `@RequestParam(defaultValue)` handles missing parameters cleanly
- Consistent behavior across all list endpoints

---

## Decision 4: Response Structure

**Decision**: Return `Page<T>` JSON directly without wrapper DTO

**Rationale**:
- `Page<T>` serializes to JSON containing all required fields
- Spring Boot 3.x `Page` JSON structure:
  ```json
  {
    "content": [...],
    "totalElements": 50,
    "totalPages": 3,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false,
    "empty": false
  }
  ```
- Extra fields (`first`, `last`, `empty`, `pageable`) are bonus - don't conflict with spec

**Alternatives Rejected**:
- **Custom PageResponse DTO**: Would require manual mapping from `Page<T>`. If spec required ONLY specific fields, this would be needed. Current approach is simpler.

---

## Decision 5: Error Handling Architecture

**Decision**: Global `@RestControllerAdvice` exception handler

**Rationale**:
- Centralized, consistent error responses
- Handles three exception types:
  1. `MethodArgumentNotValidException`: validation annotation failures
  2. `ConstraintViolationException`: parameter constraint violations
  3. `MethodArgumentTypeMismatchException`: non-numeric parameters
- Returns HTTP 400 with descriptive messages per spec requirements

---

## Performance Analysis

### Database-Level Pagination (FR-010)
- Spring Data `findAll(Pageable)` uses SQL `LIMIT/OFFSET`
- Only requested page loaded into memory
- Memory: O(pageSize), not O(totalRecords)

### Performance for 100k Records (SC-001)
- <2 second response achievable with proper indexing
- Index ORDER BY columns (typically `id` or `created_at`)
- Avoid JOIN FETCH with pagination (causes in-memory pagination)

### COUNT Query Consideration
- `Page<T>` executes separate COUNT(*) query
- For datasets >1M records with complex queries, consider:
  - Caching count results
  - Background count refresh
  - (Not implemented here - scope creep for demo app)

---

## Technology Validation

| Requirement | Spring Support | Notes |
|-------------|----------------|-------|
| page parameter | Pageable | Auto-bound from query params |
| size parameter | Pageable | Auto-bound from query params |
| Default page=0 | @RequestParam(defaultValue) | Controller configuration |
| Default size=20 | @RequestParam(defaultValue) | Controller configuration |
| Max size=100 | @Max(100) | Validation annotation |
| totalElements | Page.getTotalElements() | Built-in |
| totalPages | Page.getTotalPages() | Built-in, calculated correctly |
| HTTP 400 on invalid | @ControllerAdvice | Centralized handler |
| Database pagination | Hibernate LIMIT/OFFSET | FR-010 satisfied |

---

## Sources

- Spring Data JPA Pagination Documentation
- Baeldung: Pagination and Sorting with Spring Data JPA
- Spring Boot 3.x Validation Best Practices
- Vlad Mihalcea: Query Pagination with JPA and Hibernate
- PostgreSQL Indexing Strategies for Pagination
