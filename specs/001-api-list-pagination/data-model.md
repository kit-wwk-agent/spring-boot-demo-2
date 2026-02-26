# Data Model: API List Pagination

**Feature**: API List Pagination
**Date**: 2026-02-26

## Overview

Pagination is a cross-cutting concern that doesn't introduce new domain entities. Instead, it defines:
1. Request structure (pagination parameters)
2. Response wrapper structure (paginated results)
3. Validation rules for parameters

---

## Request Model: Pagination Parameters

### PaginationRequest (Conceptual - Spring handles via Pageable)

| Field | Type | Constraints | Default | Description |
|-------|------|-------------|---------|-------------|
| page | Integer | >= 0 | 0 | Zero-indexed page number |
| size | Integer | >= 1, <= 100 | 20 | Number of records per page |

### Validation Rules

| Rule ID | Field | Constraint | Error Message |
|---------|-------|------------|---------------|
| VR-001 | page | page >= 0 | "Page must be >= 0" |
| VR-002 | size | size >= 1 | "Size must be >= 1" |
| VR-003 | size | size <= 100 | "Size must be <= 100" |
| VR-004 | page | numeric type | "Page must be a valid integer" |
| VR-005 | size | numeric type | "Size must be a valid integer" |

---

## Response Model: PageResponse<T>

The response wraps any entity type with pagination metadata.

### Field Definitions

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| content | T[] | Query result | Array of items for current page |
| totalElements | Long | COUNT query | Total records matching criteria |
| totalPages | Integer | Calculated | ceiling(totalElements / size) |
| number | Integer | Request page | Current page number (0-indexed) |
| size | Integer | Request size | Requested page size |

### Derived Fields (from Spring Page<T>)

These are included automatically but not required by spec:

| Field | Type | Description |
|-------|------|-------------|
| first | Boolean | True if this is page 0 |
| last | Boolean | True if no more pages after this |
| empty | Boolean | True if content array is empty |
| numberOfElements | Integer | Actual items in content (may be < size on last page) |

---

## Response Examples

### Standard Response (Page 0 of 3)

```json
{
  "content": [
    { "id": 1, "name": "Item 1" },
    { "id": 2, "name": "Item 2" },
    ...
    { "id": 20, "name": "Item 20" }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

### Last Page (Partial)

```json
{
  "content": [
    { "id": 41, "name": "Item 41" },
    ...
    { "id": 50, "name": "Item 50" }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 2,
  "size": 20,
  "first": false,
  "last": true,
  "empty": false
}
```

### Empty Result

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 20,
  "first": true,
  "last": true,
  "empty": true
}
```

### Beyond Available Pages

```json
{
  "content": [],
  "totalElements": 50,
  "totalPages": 3,
  "number": 10,
  "size": 20,
  "first": false,
  "last": true,
  "empty": true
}
```

---

## Error Response Model

### ErrorResponse

| Field | Type | Description |
|-------|------|-------------|
| error | String | Error category (e.g., "Validation failed") |
| message | String | Human-readable error details |
| status | Integer | HTTP status code (400) |
| timestamp | String | ISO-8601 timestamp |

### Error Example

```json
{
  "error": "Validation failed",
  "message": "size: must be less than or equal to 100",
  "status": 400,
  "timestamp": "2026-02-26T10:30:00Z"
}
```

---

## State Transitions

Pagination is stateless - no state machine applies.

Each request is independent:
1. Client sends page/size parameters
2. Server validates parameters
3. Server queries database with LIMIT/OFFSET
4. Server returns paginated response

No session state, cursor tokens, or continuation tokens are maintained.

---

## Calculations

### totalPages Calculation (FR-008)

```
if totalElements == 0:
    totalPages = 0
else:
    totalPages = ceil(totalElements / size)
```

Examples:
- 50 elements, size 20 → 3 pages (0, 1, 2)
- 40 elements, size 20 → 2 pages (0, 1)
- 0 elements, size 20 → 0 pages
- 100 elements, size 100 → 1 page (0)

### SQL Generation

```sql
-- Data query
SELECT * FROM entity
ORDER BY id
LIMIT :size OFFSET :page * :size;

-- Count query (for totalElements)
SELECT COUNT(*) FROM entity;
```
