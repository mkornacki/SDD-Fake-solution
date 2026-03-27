# Tasks: Resilient Reservation Operations

**Input**: Design documents from `/specs/003-resilience-audit-scale/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/reservation-resilience-api.yaml ✓, quickstart.md ✓

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1–US4)
- Tests are required by FR-009, FR-014, FR-015 (constitution quality gates)

---

## Phase 1: Setup

**Purpose**: Add resilience, metrics, and queue infrastructure dependencies

- [ ] T001 Add resilience and observability dependencies to `backend/pom.xml`: Resilience4j (retry, circuit-breaker, bulkhead), Spring Retry, Micrometer (registry + actuator bridge), queue client library abstraction
- [ ] T002 [P] Configure `backend/src/main/resources/application-local.yml` with queue emulator profile, retry policy properties (`maxAttempts`, `backoffBase`, `backoffMultiplier`, `backoffMax`), and idempotency window (`idempotencyRetentionDays=30`)
- [ ] T003 [P] Configure Micrometer metrics exporter and Actuator metrics endpoint in `backend/src/main/resources/application.yml`

**Checkpoint**: Project compiles with all resilience and metrics dependencies

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain entities, persistence schema, and port interfaces required by all user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Create Clean Architecture package structure: `domain/reservation/`, `domain/idempotency/`, `domain/replay/`, `domain/audit/`, `application/command/`, `application/query/`, `application/ports/inbound/`, `application/ports/outbound/`, `adapters/inbound/http/reservation/`, `adapters/outbound/persistence/`, `adapters/outbound/messaging/`, `adapters/outbound/partner/`, `adapters/outbound/audit/`, `configuration/` under `backend/src/main/java/com/acme/reservation/`
- [ ] T005 Create Liquibase migration `V001__idempotency_request_context.sql` with `idempotency_records`, `reservation_request_contexts` tables in `backend/src/main/resources/db/migration/V001__idempotency_request_context.sql`
- [ ] T006 [P] Create Liquibase migration `V002__async_work_schema.sql` with `asynchronous_work_items`, `partner_interaction_logs` tables in `backend/src/main/resources/db/migration/V002__async_work_schema.sql`
- [ ] T007 [P] Create Liquibase migration `V003__audit_retention_schema.sql` with `audit_events`, `dead_letter_items`, `retention_policy_rules` tables in `backend/src/main/resources/db/migration/V003__audit_retention_schema.sql`
- [ ] T008 Implement `ReservationRequestContext` domain entity with status enum (`RECEIVED`, `VALIDATED`, `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`, `DLQ`) in `backend/src/main/java/com/acme/reservation/domain/reservation/ReservationRequestContext.java`
- [ ] T009 [P] Implement `IdempotencyRecord` domain entity with `expiresAt` computed from configurable `retentionDays` (default 30) in `backend/src/main/java/com/acme/reservation/domain/idempotency/IdempotencyRecord.java`
- [ ] T010 [P] Define inbound port interfaces: `CreateReservationUseCase`, `GetReservationStatusUseCase`, `ReplayDeadLetterUseCase` in `backend/src/main/java/com/acme/reservation/application/ports/inbound/`
- [ ] T011 [P] Define outbound port interfaces: `IdempotencyRepository`, `RequestContextRepository`, `WorkItemRepository`, `PartnerInteractionRepository`, `DeadLetterRepository`, `AuditEventRepository`, `RetentionPolicyRepository` in `backend/src/main/java/com/acme/reservation/application/ports/outbound/`

**Checkpoint**: Domain model, migrations, and port interfaces ready — user story work can begin

---

## Phase 3: User Story 1 — Idempotent Create Under Retry (Priority: P1) 🎯 MVP

**Goal**: Guarantee that repeated create requests for the same business context produce exactly one canonical reservation outcome, including under concurrent parallel retries.

**Independent Test**: Submit identical create requests (including concurrent) and verify one business outcome with zero duplicates; retries return consistent `contextId` and status.

### Tests for User Story 1

- [ ] T012 [P] [US1] Unit test for `IdempotencyRecord` business fingerprint generation determinism and conflict detection in `backend/src/test/java/com/acme/reservation/domain/IdempotencyRecordTest.java`
- [ ] T013 [P] [US1] Unit test for `ReservationRequestContext` state machine transitions: `RECEIVED` → `VALIDATED` → `QUEUED` → `PROCESSING` → `COMPLETED` and `PROCESSING` → `FAILED` in `backend/src/test/java/com/acme/reservation/domain/RequestContextStateTest.java`
- [ ] T014 [P] [US1] Contract test (Pact provider) for `POST /api/v1/reservations` — 202 Accepted, 200 idempotent replay, and 409 conflict on bad key reuse in `backend/src/test/java/com/acme/reservation/contract/CreateReservationContractTest.java`
- [ ] T015 [P] [US1] Integration test for concurrent duplicate create requests: unique constraint prevents duplicate `ReservationRequestContext` rows (Testcontainers + SQLite) in `backend/src/test/java/com/acme/reservation/IdempotentCreateIT.java`

### Implementation for User Story 1

- [ ] T016 [US1] Implement `CreateReservationCommandHandler` with idempotency guard (transactional upsert on `IdempotencyRecord`), `ReservationRequestContext` creation, and `AsynchronousWorkItem` enqueue in `backend/src/main/java/com/acme/reservation/application/command/CreateReservationCommandHandler.java`
- [ ] T017 [US1] Implement `IdempotencyJpaRepository` with unique constraint enforcement, `findByKey`, and expiry-aware cleanup in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/IdempotencyJpaRepository.java`
- [ ] T018 [US1] Implement `RequestContextJpaRepository` adapter (save, findByContextId, updateStatus) in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/RequestContextJpaRepository.java`
- [ ] T019 [US1] Implement `ReservationController` with `POST /api/v1/reservations` (mandatory `X-Idempotency-Key` and `X-Correlation-Id` headers) in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/ReservationController.java`
- [ ] T020 [US1] Add `GET /api/v1/reservations/{contextId}/status` to `ReservationController` for polling async processing state in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/ReservationController.java`

**Checkpoint**: User Story 1 independently testable — idempotent creates produce exactly one outcome under concurrent retries

---

## Phase 4: User Story 2 — Asynchronous Partner Processing (Priority: P2)

**Goal**: Process partner communication via durable queue with bounded exponential backoff retries, circuit-breaker protection, DLQ routing on terminal failure, and masked payload audit retention.

**Independent Test**: Submit create requests while simulating partner failures; verify retry schedule, DLQ routing with full context, and payload masking in operational views.

### Tests for User Story 2

- [X] T021 [P] [US2] Unit test for `AsynchronousWorkItem` state transitions and retry classification (transient vs. permanent) in `backend/src/test/java/com/acme/reservation/domain/WorkItemStateTest.java`
- [X] T022 [P] [US2] Unit test for exponential backoff delay calculation with jitter (base 1s, multiplier 2, max 60s) in `backend/src/test/java/com/acme/reservation/domain/RetryBackoffTest.java`
- [X] T023 [P] [US2] Unit test for `PartnerInteractionLog` PII masking: sensitive fields absent from operator-visible snapshot in `backend/src/test/java/com/acme/reservation/domain/PartnerLogMaskingTest.java`
- [X] T024 [P] [US2] Contract test (Pact consumer) for partner integration HTTP calls and expected response contract in `backend/src/test/java/com/acme/reservation/contract/PartnerIntegrationContractTest.java`
- [X] T025 [P] [US2] Integration test for transient retry progression: worker advances through `RETRY_WAIT` states, eventually reaches `SUCCEEDED` in `backend/src/test/java/com/acme/reservation/AsyncRetryIT.java`
- [X] T026 [P] [US2] Integration test for DLQ routing: after exhausting max attempts, `DeadLetterItem` is created with `replayStatus=PENDING` and failure context in `backend/src/test/java/com/acme/reservation/DlqRoutingIT.java`

### Implementation for User Story 2

- [X] T027 [US2] Implement `AsynchronousWorkItem` domain entity with retry state machine, attempt counter, `nextAttemptAt` calculation, and failure classification in `backend/src/main/java/com/acme/reservation/domain/replay/AsynchronousWorkItem.java`
- [X] T028 [US2] Implement `PartnerInteractionLog` entity with `requestSnapshotRef`, `responseSnapshotRef`, and `maskedFieldsApplied` enforcement in `backend/src/main/java/com/acme/reservation/domain/replay/PartnerInteractionLog.java`
- [X] T029 [US2] Implement `PartnerIntegrationWorker` async consumer with Resilience4j retry (exponential backoff + jitter), Resilience4j circuit-breaker, failure classification, and `PartnerInteractionLog` emission in `backend/src/main/java/com/acme/reservation/adapters/outbound/partner/PartnerIntegrationWorker.java`
- [X] T030 [US2] Implement `DlqRouter` routing terminal-failed work items to `DeadLetterItem` with masked payload reference and full attempt history in `backend/src/main/java/com/acme/reservation/adapters/outbound/messaging/DlqRouter.java`
- [X] T031 [US2] Implement `WorkItemJpaRepository` adapter (save, findReady, findForRetry, updateState) in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/WorkItemJpaRepository.java`
- [X] T032 [US2] Configure Resilience4j circuit-breaker and bulkhead for partner outbound adapter in `backend/src/main/java/com/acme/reservation/configuration/ResilienceConfig.java`

**Checkpoint**: User Stories 1 and 2 independently functional — async partner processing with retry, DLQ, and masked audit logs

---

## Phase 5: User Story 3 — Audit Security and Retention Governance (Priority: P3)

**Goal**: Capture complete immutable audit metadata for all reservation operations, enforce PII masking in all operational views, and apply configurable retention rules.

**Independent Test**: Execute reservation lifecycle actions; validate audit trails contain required fields, sensitive fields are masked everywhere, and retention policy execution complies with rules.

### Tests for User Story 3

- [X] T033 [P] [US3] Unit test for `AuditEvent` immutability: no setter methods, mandatory fields enforced at construction in `backend/src/test/java/com/acme/reservation/domain/AuditEventTest.java`
- [X] T034 [P] [US3] Unit test for `RetentionPolicyRule` disposition logic per artifact class (RAW_PAYLOAD → DELETE, AUDIT_EVENT → ARCHIVE) in `backend/src/test/java/com/acme/reservation/domain/RetentionPolicyTest.java`
- [X] T035 [P] [US3] Unit test for sensitive field masking: verify logs and exported snapshots contain no unmasked PII or credentials in `backend/src/test/java/com/acme/reservation/domain/SensitiveDataMaskingTest.java`
- [X] T036 [P] [US3] Integration test for audit event generation: create and cancel flows produce `AuditEvent` rows with actor, traceId, action, and outcome in `backend/src/test/java/com/acme/reservation/AuditTrailIT.java`

### Implementation for User Story 3

- [X] T037 [US3] Implement `AuditEvent` domain entity (append-only, UUID, actorId, actorType, action, outcome, traceId, occurredAt, beforeRef, afterRef) in `backend/src/main/java/com/acme/reservation/domain/audit/AuditEvent.java`
- [X] T038 [US3] Implement `AuditEventJpaAdapter` (append-only save, findByEntityIdAndType) outbound adapter in `backend/src/main/java/com/acme/reservation/adapters/outbound/audit/AuditEventJpaAdapter.java`
- [X] T039 [US3] Implement `RetentionPolicyRule` domain entity and `RetentionPolicyExecutor` application service cycling through artifacts beyond threshold in `backend/src/main/java/com/acme/reservation/domain/audit/RetentionPolicyRule.java`
- [X] T040 [P] [US3] Implement `AuditInterceptor` Spring AOP aspect emitting `AuditEvent` on all state-changing use-case invocations in `backend/src/main/java/com/acme/reservation/configuration/AuditInterceptor.java`

**Checkpoint**: User Stories 1, 2, and 3 independently functional — complete immutable audit with PII masking and retention governance

---

## Phase 6: User Story 4 — Scalable and Available Operations (Priority: P4)

**Goal**: Maintain critical reservation creation availability and predictable performance under heavy load through backpressure, circuit breakers, graceful degradation, and actionable observability.

**Independent Test**: Execute load and failure drills; verify circuit breaker triggers, queue depth metrics are observable, and core create path remains available when non-critical components degrade.

### Tests for User Story 4

- [X] T041 [P] [US4] Unit test for circuit-breaker open-state trigger conditions and fallback behavior in `backend/src/test/java/com/acme/reservation/adapters/CircuitBreakerTest.java`
- [X] T042 [P] [US4] Integration test for Micrometer metrics exposure: request rate, p95 latency, error rate, queue depth, consumer lag, retry count, DLQ growth, idempotency-hit ratio endpoints available via `/actuator/metrics` in `backend/src/test/java/com/acme/reservation/MetricsExposureIT.java`
- [X] T043 [P] [US4] Integration test for graceful degradation: partner component DOWN does not prevent successful create acknowledgement (202 Accepted) in `backend/src/test/java/com/acme/reservation/GracefulDegradationIT.java`

### Implementation for User Story 4

- [X] T044 [US4] Extend `ResilienceConfig` with Resilience4j bulkhead for async worker thread pool and request-rate backpressure on inbound HTTP in `backend/src/main/java/com/acme/reservation/configuration/ResilienceConfig.java`
- [X] T045 [US4] Implement `MetricsConfig` registering custom Micrometer gauges and counters: queue depth, consumer lag, retry count, DLQ growth, idempotency-hit ratio in `backend/src/main/java/com/acme/reservation/configuration/MetricsConfig.java`
- [X] T046 [US4] Instrument `PartnerIntegrationWorker` and `CreateReservationCommandHandler` with Micrometer `Timer` and `Counter` for p95 latency and error-rate metrics in `backend/src/main/java/com/acme/reservation/adapters/outbound/partner/PartnerIntegrationWorker.java`

**Checkpoint**: All four user stories independently functional — scalable, observable, and gracefully degrading

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: DLQ replay governance, end-to-end tests, idempotency expiry cleanup, and quality gate sign-off

- [X] T047 [P] Implement `DlqController` with `POST /api/v1/operations/dlq/{dlqId}/replay` — requires admin JWT scope, validates reason is non-empty, emits `AuditEvent` with operator identity and replay reason, preserves original idempotency semantics in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/DlqController.java`
- [X] T048 [P] Implement scheduled `IdempotencyRecordCleanupJob` purging expired records (beyond configurable 30-day window) in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/IdempotencyRecordCleanupJob.java`
- [X] T049 [P] E2E test for idempotent create and retry user journey: concurrent retries → one reservation → DLQ routing → replay → success in `backend/src/test/java/com/acme/reservation/e2e/IdempotentCreateE2ETest.java`
- [X] T050 [P] E2E test for DLQ replay with admin audit: replay endpoint captures operator identity, reason, and trace in audit event in `backend/src/test/java/com/acme/reservation/e2e/DlqReplayE2ETest.java`
- [X] T051 [P] E2E security test for PII masking, unauthorized DLQ replay denial, audit access control, and OWASP ASVS L3 security scenarios in `backend/src/test/java/com/acme/reservation/e2e/ResilienceSecurityE2ETest.java`
- [ ] T052 Verify SonarQube quality gate passes: zero Blocker/Critical/Major violations, domain/application layer coverage ≥ 80%
- [X] T053 [P] Document operational runbooks for scaling responses, DLQ replay procedures, and retry storm response in `specs/003-resilience-audit-scale/runbooks/operations.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **blocks all user stories**
- **Phase 3 (US1 — P1)**: Requires Phase 2 complete — **MVP, deliver first**
- **Phase 4 (US2 — P2)**: Requires Phase 2 and US1 `AsynchronousWorkItem` enqueue in place
- **Phase 5 (US3 — P3)**: Requires Phase 2 complete — audit is cross-cutting, independent of US1/US2 implementation
- **Phase 6 (US4 — P4)**: Requires Phase 2 and US2 async worker implemented (metrics instrument the worker)
- **Phase 7 (Polish)**: Requires all user story phases complete

### User Story Dependencies

- **US1 (P1)**: Standalone — idempotency guard is self-contained
- **US2 (P2)**: Depends on US1 — enqueues `AsynchronousWorkItem` created during US1 create flow
- **US3 (P3)**: Independent — audit interceptor wraps existing use-case invocations
- **US4 (P4)**: Depends on US2 — instruments the worker and queue infrastructure built in US2

### Within Each User Story

- Tests written before implementation (fail first)
- Domain entities before application handlers
- Port adapters before inbound HTTP controllers
- Configuration before instrumented components

### Parallel Opportunities

- Phase 2: T006, T007, T009–T011 run in parallel after T005 and T008
- US1: T012–T015 (tests) in parallel; T017–T018 (JPA adapters) in parallel
- US2: T021–T026 (tests) in parallel; T027–T028 (domain entities) in parallel
- US3: T033–T036 (tests) in parallel; T039–T040 in parallel
- US4: T041–T043 (tests) in parallel
- Phase 7: T047–T051, T053 in parallel

---

## Parallel Example: User Story 1 (MVP)

```
T012 ──┐
T013 ──┤
T014 ──┼──► pass in parallel ──► T016 ──► T017 ──┐
T015 ──┘                                T018 ──┤──► T019 ──► T020
```

---

## Parallel Example: User Story 2 (Async Processing)

```
T021 ──┐
T022 ──┤
T023 ──┤──► pass in parallel ──► T027 ──► T028 ──► T029 ──► T030 ──► T031 ──► T032
T024 ──┤
T025 ──┤
T026 ──┘
```

---

## Implementation Strategy

- **MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1) — idempotent create with zero duplicate guarantee
- **Incremental delivery**: US2 (async + retry + DLQ), US3 (audit governance), US4 (observability/scale) as independent increments
- **Each task describes a single file** with sufficient context for LLM implementation without additional input

---

## Task Summary

| Phase | Tasks | User Story | Parallel Tasks |
|-------|-------|------------|----------------|
| Phase 1 — Setup | T001–T003 | — | T002, T003 |
| Phase 2 — Foundational | T004–T011 | — | T006, T007, T009–T011 |
| Phase 3 — US1 (P1) | T012–T020 | US1 | T012–T015, T017–T018 |
| Phase 4 — US2 (P2) | T021–T032 | US2 | T021–T026, T027–T028 |
| Phase 5 — US3 (P3) | T033–T040 | US3 | T033–T036, T039–T040 |
| Phase 6 — US4 (P4) | T041–T046 | US4 | T041–T043 |
| Phase 7 — Polish | T047–T053 | — | T047–T051, T053 |

**Total tasks**: 53  
**Total parallelizable**: 34  
**MVP subset (Phase 1–3)**: 20 tasks
