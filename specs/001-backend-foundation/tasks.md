# Tasks: Backend Foundation REST API

**Input**: Design documents from `/specs/001-backend-foundation/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/foundation-api.yaml ✓, quickstart.md ✓

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Tests are required per FR-017 and FR-019; included in each user story phase

---

## Phase 1: Setup

**Purpose**: Initialize Spring Boot project with all required tooling

- [X] T001 Initialize Spring Boot project skeleton with Maven wrapper in `backend/`
- [X] T002 Configure `backend/pom.xml` with BOM-managed dependencies: Spring Boot, Spring Web MVC, Spring Security (OAuth2 Resource Server), Spring Data JPA, Hibernate, Liquibase, springdoc-openapi, JUnit 5, Mockito, Spring Boot Test, Testcontainers, Pact, Playwright runner
- [X] T003 [P] Configure Checkstyle (Google Java Style) plugin in `backend/pom.xml` with ruleset at `backend/checkstyle.xml`
- [X] T004 [P] Configure SonarQube Maven plugin and quality gate properties in `backend/pom.xml`
- [X] T005 [P] Create externalized configuration files: `backend/src/main/resources/application.yml`, `application-local.yml`, `application-prod.yml` with Kubernetes-compatible env-variable placeholders

**Checkpoint**: Project compiles, dependencies resolved, quality gate plugin configured

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that must complete before any user story implementation

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 Create Clean Architecture package structure: `domain/`, `application/ports/inbound/`, `application/ports/outbound/`, `adapters/inbound/http/`, `adapters/outbound/persistence/`, `configuration/` under `backend/src/main/java/com/acme/foundation/`
- [X] T007 [P] Create initial Liquibase changelog and first migration in `backend/src/main/resources/db/migration/V001__initial_schema.sql` (audit_events table)
- [X] T008 Configure Spring Security as OAuth 2.0/OIDC Resource Server with JWT validation in `backend/src/main/java/com/acme/foundation/configuration/SecurityConfig.java`
- [X] T009 [P] Implement global RFC 9457 exception handler covering validation, auth, and unexpected errors in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/error/GlobalExceptionHandler.java`
- [X] T010 [P] Configure structured JSON logging with Logback: `traceId`, `spanId`, `level`, `message`, `requestId` fields, PII excluded — `backend/src/main/resources/logback-spring.xml`
- [X] T011 [P] Configure springdoc-openapi with profile-based disabling in production in `backend/src/main/java/com/acme/foundation/configuration/OpenApiConfig.java`
- [X] T012 [P] Create standard `ApiResponse<T>` response envelope in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/model/ApiResponse.java`
- [X] T013 [P] Create `ProblemDetailResponse` model (RFC 9457 fields: type, title, status, detail, instance, extensions) in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/model/ProblemDetailResponse.java`
- [X] T014 [P] Configure `X-Correlation-Id` request header propagation and MDC binding in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/filter/CorrelationIdFilter.java`

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel

---

## Phase 3: User Story 1 — Consume Core API Endpoints (Priority: P1) 🎯 MVP

**Goal**: Expose versioned REST endpoints returning consistent JSON responses with RFC 9457 error handling, testable without frontend.

**Independent Test**: Call documented endpoints with valid and invalid requests; verify JSON success envelopes and RFC 9457 problem detail error responses.

### Tests for User Story 1

- [X] T015 [P] [US1] Unit test for `ApiResponse` envelope construction with data and meta fields in `backend/src/test/java/com/acme/foundation/adapters/http/ApiResponseTest.java`
- [X] T016 [P] [US1] Unit test for `GlobalExceptionHandler` RFC 9457 field mapping for all error types in `backend/src/test/java/com/acme/foundation/adapters/http/GlobalExceptionHandlerTest.java`
- [X] T017 [P] [US1] Contract test (Pact provider) for `GET /api/v1/health` success response in `backend/src/test/java/com/acme/foundation/contract/HealthEndpointContractTest.java`
- [X] T018 [P] [US1] Integration test for authenticated endpoint returns HTTP 200 with JSON envelope in `backend/src/test/java/com/acme/foundation/adapters/http/HealthControllerIT.java`

### Implementation for User Story 1

- [X] T019 [US1] Implement `HealthSummaryUseCase` inbound port in `backend/src/main/java/com/acme/foundation/application/ports/inbound/HealthSummaryUseCase.java`
- [X] T020 [US1] Implement `HealthSummaryService` application service in `backend/src/main/java/com/acme/foundation/application/HealthSummaryService.java`
- [X] T021 [US1] Implement `HealthController` with authenticated `GET /api/v1/health` returning `ApiResponse` in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/health/HealthController.java`
- [X] T022 [US1] Wire input validation with `@Valid`, map `MethodArgumentNotValidException` and `ConstraintViolationException` to RFC 9457 in `GlobalExceptionHandler` (`backend/src/main/java/com/acme/foundation/adapters/inbound/http/error/GlobalExceptionHandler.java`)
- [X] T023 [US1] Map unsupported media type, method-not-allowed, and unknown-route errors to RFC 9457 in `GlobalExceptionHandler`

**Checkpoint**: User Story 1 independently testable — REST endpoint returns JSON, errors return RFC 9457

---

## Phase 4: User Story 2 — Enforce Secure API Access (Priority: P2)

**Goal**: Every endpoint requires a valid JWT bearer token; unauthenticated and unauthorized requests are rejected with RFC 9457 JSON errors.

**Independent Test**: Call every endpoint with and without valid credentials; verify only authenticated, authorized requests succeed.

### Tests for User Story 2

- [X] T024 [P] [US2] Unit test for `AuthenticationContext` value object extraction and scope validation in `backend/src/test/java/com/acme/foundation/domain/auth/AuthenticationContextTest.java`
- [X] T025 [P] [US2] Integration test for unauthenticated request returns HTTP 401 with RFC 9457 body in `backend/src/test/java/com/acme/foundation/security/UnauthenticatedRequestIT.java`
- [X] T026 [P] [US2] Integration test for insufficient-scope request returns HTTP 403 with RFC 9457 body in `backend/src/test/java/com/acme/foundation/security/AuthorizationIT.java`
- [X] T027 [P] [US2] Integration test for expired/malformed token returns HTTP 401 with RFC 9457 body in `backend/src/test/java/com/acme/foundation/security/InvalidTokenIT.java`

### Implementation for User Story 2

- [X] T028 [US2] Implement `AuthenticationContext` domain value object (subjectId, scopes, issuer, expiry) in `backend/src/main/java/com/acme/foundation/domain/audit/AuthenticationContext.java`
- [X] T029 [US2] Configure Spring Security filter chain: require bearer token on all `non-actuator` endpoints, return RFC 9457 for 401/403 via `AuthenticationEntryPoint` and `AccessDeniedHandler` in `backend/src/main/java/com/acme/foundation/configuration/SecurityConfig.java`
- [X] T030 [US2] Implement `AuditEvent` domain entity (append-only, mandatory fields: actorId, action, outcome, traceId, occurredAt) in `backend/src/main/java/com/acme/foundation/domain/audit/AuditEvent.java`
- [X] T031 [US2] Implement `AuditEventRepository` outbound port and JPA adapter in `backend/src/main/java/com/acme/foundation/adapters/outbound/persistence/AuditEventJpaAdapter.java`
- [X] T032 [US2] Integrate audit event publication on endpoint access and auth failure in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/filter/AuditLoggingFilter.java`

**Checkpoint**: User Stories 1 and 2 independently testable — all endpoints require auth, errors are RFC 9457

---

## Phase 5: User Story 3 — Validate Operational Readiness Baseline (Priority: P3)

**Goal**: The service exposes unauthenticated Kubernetes health probes (liveness/readiness) and emits structured JSON logs without PII.

**Independent Test**: Deploy in representative environment; verify health probes return machine-readable JSON and logs are structured without sensitive fields.

### Tests for User Story 3

- [X] T033 [P] [US3] Unit test for `HealthStatus` value object construction and DOWN propagation logic in `backend/src/test/java/com/acme/foundation/domain/health/HealthStatusTest.java`
- [X] T034 [P] [US3] Integration test for `GET /actuator/health/liveness` returns HTTP 200 without auth in `backend/src/test/java/com/acme/foundation/health/LivenessProbeIT.java`
- [X] T035 [P] [US3] Integration test for `GET /actuator/health/readiness` returns HTTP 200 without auth in `backend/src/test/java/com/acme/foundation/health/ReadinessProbeIT.java`
- [X] T036 [P] [US3] Integration test verifying structured log output contains required JSON fields and no PII in `backend/src/test/java/com/acme/foundation/logging/StructuredLoggingIT.java`
- [X] T037 [P] [US3] Integration test verifying OpenAPI docs return HTTP 200 on `local` profile and HTTP 404 on `prod` profile in `backend/src/test/java/com/acme/foundation/openapi/OpenApiProfileIT.java`

### Implementation for User Story 3

- [X] T038 [US3] Configure Spring Boot Actuator with liveness and readiness probes, expose on management port, exclude from authentication in `backend/src/main/java/com/acme/foundation/configuration/ActuatorConfig.java`
- [X] T039 [US3] Implement `DatabaseHealthIndicator` custom health check for SQLite connectivity in `backend/src/main/java/com/acme/foundation/adapters/outbound/persistence/DatabaseHealthIndicator.java`
- [X] T040 [US3] Verify `application-prod.yml` sets `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`

**Checkpoint**: All three user stories independently functional — health probes, auth, structured logs, RFC 9457 errors

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation, security hardening, quality gate sign-off

- [X] T041 [P] E2E test for critical user flow: obtain JWT → call `GET /api/v1/health` → verify JSON response envelope in `backend/src/test/java/com/acme/foundation/e2e/FoundationE2ETest.java`
- [X] T042 [P] E2E security test for OWASP ASVS L3 scenarios: missing auth, expired token, injected headers, SQL injection attempt — in `backend/src/test/java/com/acme/foundation/e2e/SecurityE2ETest.java`
- [X] T043 [P] Validate all endpoints in `contracts/foundation-api.yaml` are implemented, documented in OpenAPI, and return RFC 9457 on error
- [ ] T044 Verify SonarQube quality gate passes: zero Blocker/Critical/Major violations, domain coverage ≥ 80%
- [X] T045 [P] Run quickstart.md validation scenarios locally and confirm all 6 scenarios pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **blocks all user stories**
- **Phase 3 (US1 — P1)**: Requires Phase 2 complete — **MVP, deliver first**
- **Phase 4 (US2 — P2)**: Requires Phase 2 complete — can run in parallel with Phase 3 if staffed
- **Phase 5 (US3 — P3)**: Requires Phase 2 complete — can run in parallel with Phases 3 and 4 if staffed
- **Phase 6 (Polish)**: Requires all desired user story phases complete

### User Story Dependencies

- **US1 (P1)**: No dependency on US2 or US3
- **US2 (P2)**: No dependency on US1 or US3 (security is foundational but tested independently)
- **US3 (P3)**: No dependency on US1 or US2

### Within Each User Story

- Tests MUST be written before implementation (fail first)
- Ports (interfaces) before adapters
- Domain entities before application services
- Services before HTTP controllers

### Parallel Opportunities

- Phase 1: T003, T004, T005 can run in parallel after T001 and T002
- Phase 2: T007–T014 can run in parallel after T006 (package structure)
- Each user story: tests marked [P] can run in parallel within the story
- US1, US2, US3 can be developed in parallel by separate team members once Phase 2 is complete

---

## Parallel Example: User Story 1 (MVP)

```
T015 ──┐
T016 ──┤
T017 ──┼──► all pass in parallel ──► T019 ──► T020 ──► T021 ──► T022 ──► T023
T018 ──┘
```

---

## Implementation Strategy

- **MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1) — delivers a working authenticated REST endpoint with RFC 9457 errors
- **Incremental delivery**: Add US2 (security hardening) and US3 (health probes + logging) as independent increments
- **All tasks are independently executable**: each task describes a single file with context sufficient for an LLM to implement without additional input

---

## Task Summary

| Phase | Tasks | User Story | Parallel Tasks |
|-------|-------|------------|----------------|
| Phase 1 — Setup | T001–T005 | — | T003, T004, T005 |
| Phase 2 — Foundational | T006–T014 | — | T007–T014 |
| Phase 3 — US1 (P1) | T015–T023 | US1 | T015–T018 |
| Phase 4 — US2 (P2) | T024–T032 | US2 | T024–T027 |
| Phase 5 — US3 (P3) | T033–T040 | US3 | T033–T037 |
| Phase 6 — Polish | T041–T045 | — | T041–T043, T045 |

**Total tasks**: 45  
**Total parallelizable**: 27  
**MVP subset (Phase 1–3)**: 23 tasks
