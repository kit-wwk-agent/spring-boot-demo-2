# spring-boot-demo-2--2e2c4b7a Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-24

## Active Technologies
- Java 21 (LTS) + spring-boot-starter-web, spring-boot-starter-actuator, spring-boot-starter-data-jpa (001-api-rate-limiting)
- In-memory (ConcurrentHashMap for rate limit counters); H2 for dev, PostgreSQL for prod (existing, not used for rate limiting) (001-api-rate-limiting)
- Java 21 (LTS) + spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, spring-boot-starter-actuator (001-api-list-pagination)
- H2 (dev), PostgreSQL (prod) via Spring Data JPA (001-api-list-pagination)
- Java 21 (LTS) + Spring Boot 3.4.3 (spring-boot-starter-actuator, spring-boot-starter-data-jpa, spring-boot-starter-web) (005-actuator-health)
- H2 (dev/test), PostgreSQL (prod) via Spring Data JPA (005-actuator-health)
- Java 21 (LTS) with Spring Boot 3.4.3 + spring-boot-starter-actuator, spring-boot-starter-data-jpa, spring-boot-starter-web (006-actuator-health)

- Java 21 (LTS) + spring-boot-starter-actuator, spring-boot-starter-data-jpa, spring-boot-starter-web (001-actuator-health)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 21 (LTS)

## Code Style

Java 21 (LTS): Follow standard conventions

## Recent Changes
- 006-actuator-health: Added Java 21 (LTS) with Spring Boot 3.4.3 + spring-boot-starter-actuator, spring-boot-starter-data-jpa, spring-boot-starter-web
- 005-actuator-health: Added Java 21 (LTS) + Spring Boot 3.4.3 (spring-boot-starter-actuator, spring-boot-starter-data-jpa, spring-boot-starter-web)
- 001-api-list-pagination: Added Java 21 (LTS) + spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, spring-boot-starter-actuator


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
