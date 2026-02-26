# Quickstart: API List Pagination

**Feature**: API List Pagination
**Estimated Implementation**: 2-4 hours

## Prerequisites

- Java 21 installed
- Project builds successfully: `./gradlew build`
- Existing list endpoints to paginate (or new ones to create)

---

## Implementation Steps

### Step 1: Configure Validation

Enable method-level validation in your application.

**application.properties** (or .yml):
```properties
# Already using spring-boot-starter-validation - no additional config needed
```

### Step 2: Create Error Response DTO

**src/main/java/com/example/demo/dto/ErrorResponse.java**:
```java
package com.example.demo.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    String error,
    String message,
    int status,
    LocalDateTime timestamp
) {
    public ErrorResponse(String error, String message, int status) {
        this(error, message, status, LocalDateTime.now());
    }
}
```

### Step 3: Create Global Exception Handler

**src/main/java/com/example/demo/exception/GlobalExceptionHandler.java**:
```java
package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
            .collect(Collectors.joining(", "));
        return new ErrorResponse("Validation failed", message, 400);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
            "Parameter '%s' must be %s, but got '%s'",
            ex.getName(),
            ex.getRequiredType().getSimpleName(),
            ex.getValue()
        );
        return new ErrorResponse("Type Mismatch Error", message, 400);
    }
}
```

### Step 4: Add Pagination to Controller

**Example: Paginated List Endpoint**:
```java
package com.example.demo.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@Validated
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public Page<Item> listItems(
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {

        Pageable pageable = PageRequest.of(page, size);
        return itemRepository.findAll(pageable);
    }
}
```

### Step 5: Write Tests

**src/test/java/com/example/demo/controller/ItemControllerTest.java**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenItems_whenRequestPage0Size20_thenReturns20Items() throws Exception {
        mockMvc.perform(get("/api/items")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void givenNoParams_whenRequest_thenUsesDefaults() throws Exception {
        mockMvc.perform(get("/api/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void givenSizeOver100_whenRequest_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/items")
                .param("size", "150"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void givenNegativePage_whenRequest_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/items")
                .param("page", "-1"))
            .andExpect(status().isBadRequest());
    }
}
```

---

## Verification Checklist

- [ ] `./gradlew build` passes
- [ ] GET `/api/items?page=0&size=20` returns paginated response
- [ ] GET `/api/items` (no params) uses defaults page=0, size=20
- [ ] GET `/api/items?size=150` returns 400 error
- [ ] GET `/api/items?page=-1` returns 400 error
- [ ] GET `/api/items?page=abc` returns 400 error
- [ ] Response includes `content`, `totalElements`, `totalPages`, `number`, `size`

---

## Common Issues

### Issue: ConstraintViolationException returns 500

**Solution**: Add `@Validated` annotation to controller class (not just method).

### Issue: Page returns all records

**Solution**: Ensure repository extends `JpaRepository` or `PagingAndSortingRepository`, and pass `Pageable` to query method.

### Issue: Non-numeric params return 500

**Solution**: Add `MethodArgumentTypeMismatchException` handler to `@ControllerAdvice`.

---

## Next Steps

After basic pagination works:
1. Add sorting support via `Sort` parameter
2. Consider caching count queries for large datasets
3. Add API documentation with SpringDoc/Swagger
