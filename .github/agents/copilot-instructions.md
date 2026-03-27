# Hrs_assessment Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-26

## Active Technologies
- Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Web, Spring Validation, Spring Data JPA, Hibernate, resilience patterns (retry/backoff/circuit breaker via Spring ecosystem), OpenAPI tooling, Pact (or equivalent) for contract tests (003-resilience-audit-scale)
- SQLite for development/test, relational persistence via JPA/Hibernate; durable queue/broker abstraction for async work and DLQ (003-resilience-audit-scale)
- Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Web MVC, Spring Security (OAuth 2.0 / OIDC Resource Server), Spring Data JPA, Hibernate, Liquibase (schema migrations), OpenAPI/Swagger (springdoc-openapi), Pact (contract tests), SonarQube quality gate integration (001-backend-foundation)
- SQLite for development/test via JPA/Hibernate; schema managed by Liquibase migrations (001-backend-foundation)
- Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Web MVC, Spring Security (OAuth 2.0 / OIDC Resource Server), Spring Data JPA, Hibernate, Liquibase, springdoc-openapi, Spring Retry / Resilience4j (exponential backoff, circuit breaker), durable queue abstraction, Pact (contract tests), SonarQube (002-hotel-reservation-api)
- SQLite for development/test via JPA/Hibernate; durable message queue/broker abstraction for async processing and DLQ (002-hotel-reservation-api)
- Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Data JPA, Hibernate, Flyway or Liquibase for versioned migrations, Docker Compose, SQLite JDBC driver, Spring Boot Actuator (004-dev-environment-bootstrap)
- SQLite as the local development database, persisted via mounted volume/file in the application container (004-dev-environment-bootstrap)

- Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Web, Spring Data JPA, Hibernate, validation, resilience library (retry/circuit breaker), OpenAPI tooling (003-resilience-audit-scale)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

# Add commands for Java 11

## Code Style

Java 11: Follow standard conventions

## Recent Changes
- 004-dev-environment-bootstrap: Added Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Data JPA, Hibernate, Flyway or Liquibase for versioned migrations, Docker Compose, SQLite JDBC driver, Spring Boot Actuator
- 003-resilience-audit-scale: Added Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Web, Spring Validation, Spring Data JPA, Hibernate, resilience patterns (retry/backoff/circuit breaker via Spring ecosystem), OpenAPI tooling, Pact (or equivalent) for contract tests
- 002-hotel-reservation-api: Added Java 11 + Spring Boot (latest stable compatible with Java 11), Spring Web MVC, Spring Security (OAuth 2.0 / OIDC Resource Server), Spring Data JPA, Hibernate, Liquibase, springdoc-openapi, Spring Retry / Resilience4j (exponential backoff, circuit breaker), durable queue abstraction, Pact (contract tests), SonarQube


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
