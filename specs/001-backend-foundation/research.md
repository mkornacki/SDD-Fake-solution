# Research: Backend Foundation REST API

## Decision 1: Language and framework
- Decision: Java 11 with the latest stable Spring Boot version compatible with Java 11.
- Rationale: Mandated by the project constitution. Provides full Spring ecosystem support for REST, security, persistence, and testing.
- Alternatives considered: Other JVM languages (rejected by constitution); newer Java versions (rejected because constitution mandates Java 11).

## Decision 2: Clean Architecture module boundaries
- Decision: Apply Clean Architecture with domain, application (use-case/ports), and adapter layers. Domain layer has zero dependencies on Spring, Hibernate, or any framework artifact.
- Rationale: The constitution mandates this separation to prevent direct coupling between modules and ensure testability of business logic in isolation.
- Alternatives considered: Layered architecture without dependency inversion (rejected by constitution); feature-slice-only structure (rejected because it does not enforce the interface boundary required by the constitution).

## Decision 3: Authentication and authorization model
- Decision: Spring Security configured as an OAuth 2.0 / OIDC Resource Server, validating JWT tokens issued by an external identity provider. Every endpoint requires a valid bearer token; no anonymous endpoints defined.
- Rationale: The constitution mandates authentication on every endpoint and recommends OAuth 2.0 / OIDC. JWT validation is stateless and compatible with horizontal scaling.
- Alternatives considered: Session-based authentication (rejected due to stateful requirements conflicting with 12-factor/Kubernetes constraints); API key only (rejected as it does not support role/scope-based authorization required by constitution).

## Decision 4: API error model
- Decision: Implement a global exception handler returning RFC 9457 Problem Details JSON (`application/problem+json`) for all error scenarios, including validation failures, authentication/authorization errors, and unexpected failures.
- Rationale: FR-004 and the constitution require RFC 9457 format. Consistent error envelopes simplify client integration.
- Alternatives considered: Custom JSON error format (rejected because it conflicts with FR-004 and constitution compliance); Spring default error responses (rejected because they do not conform to RFC 9457 structure).

## Decision 5: API documentation strategy
- Decision: Use springdoc-openapi to auto-generate OpenAPI 3.x documentation served at `/swagger-ui.html` and `/v3/api-docs`. Documentation is enabled only when the `non-prod` or `local` Spring profile is active. The production profile disables all documentation endpoints.
- Rationale: FR-014 requires full API documentation in non-production; FR-015 forbids it in production. Profile-based conditional exposure is the simplest compliant approach.
- Alternatives considered: Manually maintained OpenAPI YAML outside the service (rejected due to drift risk); always-on documentation (rejected because it violates FR-015 and constitution security).

## Decision 6: Structured logging
- Decision: Configure Logback with JSON structured output to stdout, including `traceId`, `spanId`, `level`, `message`, and `requestId` fields. Sensitive and personal data is forbidden in all log statements per FR-008 and constitution section 5.
- Rationale: The constitution and FR-009 require structured stdout logs suitable for centralized aggregation. JSON format integrates cleanly with Kubernetes log shipping pipelines.
- Alternatives considered: Plain-text log format (rejected due to parsing complexity in aggregators); synchronous log shipping to external system (rejected because constitution mandates stdout).

## Decision 7: Health and readiness probes
- Decision: Expose `/actuator/health/liveness` and `/actuator/health/readiness` via Spring Boot Actuator with Kubernetes probe annotations. Health endpoints return machine-readable JSON status without requiring authentication.
- Rationale: FR-010 requires readiness and liveness endpoints for orchestration. Kubernetes cannot probe authenticated endpoints without additional complexity.
- Alternatives considered: Custom health controller (rejected in favor of Actuator which is already Spring Boot maintained); single combined health endpoint (rejected because Kubernetes requires separate liveness and readiness signals).

## Decision 8: Schema migration strategy
- Decision: Use Liquibase for versioned database migrations. All migrations are stored under `src/main/resources/db/migration/` as numbered changesets. Migrations run automatically on startup.
- Rationale: The constitution requires versioned and repeatable schema changes. Liquibase integrates cleanly with Spring Boot and produces auditable migration history.
- Alternatives considered: Flyway (equivalent; Liquibase chosen for XML/YAML changelog flexibility); manual schema management (rejected because it lacks version tracking and repeatability guarantees).

## Decision 9: Testing strategy
- Decision: Apply testing pyramid: extensive unit tests for all domain logic, integration tests using Spring Boot Test and Testcontainers for persistence and adapter boundaries, Pact-based contract tests for HTTP consumers/providers, and Playwright end-to-end tests for critical user flows and OWASP ASVS security scenarios.
- Rationale: The constitution mandates all four test layers, SonarQube quality gate enforcement, and isolated/deterministic test execution.
- Alternatives considered: Integration-test-heavy strategy (rejected because unit test coverage of domain logic is mandated); skipping contract tests (rejected because the constitution makes them mandatory CI/CD stages).

## Decision 10: Versioning and backward compatibility
- Decision: API versioning via URL path prefix (`/api/v1/`). Published contract versions are maintained within a defined support window before deprecation. Semantic Versioning 2.0.0 applied to the service artifact.
- Rationale: FR-013 requires versioned API contracts and backward compatibility. URL-based versioning is explicit and easily routed by API gateways.
- Alternatives considered: Header-based versioning (rejected due to discoverability and caching complexity); no versioning strategy (rejected because FR-013 mandates it).
