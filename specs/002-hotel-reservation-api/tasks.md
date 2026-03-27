# Tasks: Hotel Reservation Partner API

**Input**: Design documents from `/specs/002-hotel-reservation-api/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/reservation-api.yaml ✓, quickstart.md ✓

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1–US4)
- Tests are required per FR-020; included in each user story phase

---

## Phase 1: Setup

**Purpose**: Extend the backend project with reservation feature dependencies and async infrastructure

- [x] T001 Add reservation feature dependencies to `backend/pom.xml`: Resilience4j (retry/circuit-breaker/bulkhead), Spring Retry, queue client library (Spring AMQP or similar abstraction), Micrometer metrics
- [x] T002 [P] Extend `backend/src/main/resources/application-local.yml` with async worker queue profile, partner stub endpoint, and retry policy configuration properties
- [x] T003 [P] Create Conventional Commits git hook configuration in `.git/hooks/commit-msg` and document in `backend/CONTRIBUTING.md`

**Checkpoint**: Project compiles with all reservation dependencies

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain aggregates, persistence schema, port interfaces, and async infrastructure that MUST be in place before any user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create Clean Architecture package structure for reservation domain: `domain/reservation/`, `domain/cancellation/`, `domain/financial/`, `domain/idempotency/`, `domain/audit/`, `application/command/`, `application/query/`, `application/ports/inbound/`, `application/ports/outbound/`, `adapters/inbound/http/reservation/`, `adapters/outbound/persistence/`, `adapters/outbound/messaging/`, `adapters/outbound/partner/`, `configuration/` under `backend/src/main/java/com/acme/reservation/`
- [x] T005 Create Liquibase migration `V001__reservation_schema.sql` with `reservations`, `room_reservation_items` tables in `backend/src/main/resources/db/migration/V001__reservation_schema.sql`
- [x] T006 [P] Create Liquibase migration `V002__idempotency_integration_schema.sql` with `idempotency_records`, `integration_tasks`, `dlq_items` tables in `backend/src/main/resources/db/migration/V002__idempotency_integration_schema.sql`
- [x] T007 [P] Create Liquibase migration `V003__audit_history_schema.sql` with `reservation_history_events`, `financial_breakdowns` tables in `backend/src/main/resources/db/migration/V003__audit_history_schema.sql`
- [x] T008 Implement `ReservationStatus` and `RoomStatus` enums in `backend/src/main/java/com/acme/reservation/domain/reservation/ReservationStatus.java` and `RoomStatus.java`
- [x] T009 Implement `Reservation` aggregate root with `@Version` optimistic lock, state transition guards, and financial aggregate derivation in `backend/src/main/java/com/acme/reservation/domain/reservation/Reservation.java`
- [x] T010 [P] Implement `RoomReservationItem` entity with status machine, date validation, and price fields in `backend/src/main/java/com/acme/reservation/domain/reservation/RoomReservationItem.java`
- [x] T011 [P] Implement `ReservationHistoryEvent` append-only domain entity (eventId, reservationId, roomItemId, eventType, actorId, actorType, reason, traceId, occurredAt) in `backend/src/main/java/com/acme/reservation/domain/reservation/ReservationHistoryEvent.java`
- [x] T012 [P] Implement `IdempotencyRecord` domain entity with unique key constraint, lifecycle enum, and configurable `expiresAt` in `backend/src/main/java/com/acme/reservation/domain/idempotency/IdempotencyRecord.java`
- [x] T013 [P] Define inbound port interfaces: `CreateReservationUseCase`, `CancelReservationUseCase`, `CancelRoomUseCase`, `GetReservationUseCase` in `backend/src/main/java/com/acme/reservation/application/ports/inbound/`
- [x] T014 [P] Define outbound port interfaces: `ReservationRepository`, `IdempotencyRepository`, `IntegrationTaskRepository`, `DlqRepository`, `AuditEventRepository` in `backend/src/main/java/com/acme/reservation/application/ports/outbound/`
- [x] T015 [P] Implement PII field-level encryption/tokenization service in `backend/src/main/java/com/acme/reservation/adapters/outbound/pii/PiiEncryptionService.java`
- [x] T016 [P] Configure async worker thread pool and durable queue consumer bean in `backend/src/main/java/com/acme/reservation/configuration/AsyncWorkerConfig.java`

**Checkpoint**: Domain model, migrations, port interfaces, and async config ready — user story work can begin

---

## Phase 3: User Story 1 — Create Multi-Room Reservation (Priority: P1) 🎯 MVP

**Goal**: Accept a multi-room reservation in a single idempotent request, return a deterministic `reservationId`, and process partner communication asynchronously.

**Independent Test**: Submit valid and duplicate create requests with multiple rooms; verify deterministic `reservationId`, idempotent retry behavior, and asynchronous status progression.

### Tests for User Story 1

- [x] T017 [P] [US1] Unit test for `Reservation` aggregate creation invariants (at least one room, consistent totals) in `backend/src/test/java/com/acme/reservation/domain/ReservationCreateTest.java`
- [x] T018 [P] [US1] Unit test for `IdempotencyRecord` duplicate detection and IN_PROGRESS → COMPLETED lifecycle in `backend/src/test/java/com/acme/reservation/domain/IdempotencyRecordTest.java`
- [x] T019 [P] [US1] Contract test (Pact provider) for `POST /api/v1/reservations` 202 and 200 (idempotent replay) responses in `backend/src/test/java/com/acme/reservation/contract/CreateReservationContractTest.java`
- [x] T020 [P] [US1] Integration test for idempotent create: concurrent duplicate requests produce one reservation (Testcontainers + SQLite) in `backend/src/test/java/com/acme/reservation/CreateReservationIT.java`

### Implementation for User Story 1

- [x] T021 [US1] Implement `CreateReservationCommandHandler` with idempotency guard (transactional upsert), `Reservation` aggregate construction, and `IntegrationTask` enqueue in `backend/src/main/java/com/acme/reservation/application/command/CreateReservationCommandHandler.java`
- [x] T022 [US1] Implement `ReservationJpaRepository` adapter (save, findById) in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/ReservationJpaRepository.java`
- [x] T023 [US1] Implement `IdempotencyJpaRepository` adapter with unique constraint enforcement and `findByKey` in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/IdempotencyJpaRepository.java`
- [x] T024 [US1] Implement `IntegrationTaskJpaRepository` adapter (save, findPending) in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/IntegrationTaskJpaRepository.java`
- [x] T025 [US1] Implement `ReservationController` with `POST /api/v1/reservations` (requires `X-Idempotency-Key` and `Authorization` headers) in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/ReservationController.java`

**Checkpoint**: User Story 1 independently functional — multi-room reservation created idempotently with async processing

---

## Phase 4: User Story 2 — View Reservation State and History (Priority: P2)

**Goal**: Retrieve the current reservation state, financial breakdown, room items, and full change history; PII masked for callers without PII scope.

**Independent Test**: Retrieve reservations after creation; validate current state, history events, and PII masking based on caller scope.

### Tests for User Story 2

- [x] T026 [P] [US2] Unit test for `ReservationHistoryEvent` immutability (no setter methods, mandatory fields enforced) in `backend/src/test/java/com/acme/reservation/domain/ReservationHistoryEventTest.java`
- [x] T027 [P] [US2] Unit test for `PIIAccessContext` masking logic (guest fields masked when `hasPIIAccess=false`) in `backend/src/test/java/com/acme/reservation/adapters/http/PIIMaskingTest.java`
- [x] T028 [P] [US2] Contract test (Pact provider) for `GET /api/v1/reservations/{reservationId}` response schema with and without PII scope in `backend/src/test/java/com/acme/reservation/contract/GetReservationContractTest.java`
- [x] T029 [P] [US2] Integration test for reservation retrieval including history and financial breakdown in `backend/src/test/java/com/acme/reservation/GetReservationIT.java`

### Implementation for User Story 2

- [x] T030 [US2] Implement `GetReservationQueryHandler` assembling reservation, room items, history, and financial breakdown in `backend/src/main/java/com/acme/reservation/application/query/GetReservationQueryHandler.java`
- [x] T031 [US2] Implement `PIIAccessContext` (derived from JWT scopes, `hasPIIAccess` flag) in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/PIIAccessContext.java`
- [x] T032 [US2] Implement `ReservationResponseMapper` applying PII masking based on `PIIAccessContext` in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/ReservationResponseMapper.java`
- [x] T033 [US2] Add `GET /api/v1/reservations/{reservationId}` to `ReservationController` in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/ReservationController.java`
- [x] T034 [US2] Implement `HistoryEventJpaRepository` adapter (findByReservationIdOrderByOccurredAtAsc) in `backend/src/main/java/com/acme/reservation/adapters/outbound/persistence/HistoryEventJpaRepository.java`

**Checkpoint**: User Stories 1 and 2 independently functional — create and retrieve flow complete with PII masking

---

## Phase 5: User Story 3 — Cancel Individual Rooms Safely (Priority: P3)

**Goal**: Cancel a single room item within a multi-room reservation atomically, recalculate financial aggregates, and leave other rooms unaffected.

**Independent Test**: Cancel one room in a multi-room reservation; verify atomic aggregate update, history event, and unchanged status of remaining rooms.

### Tests for User Story 3

- [x] T035 [P] [US3] Unit test for partial cancellation aggregate invariants (only targeted room transitions, others remain ACTIVE) in `backend/src/test/java/com/acme/reservation/domain/PartialCancellationTest.java`
- [x] T036 [P] [US3] Unit test for `FinancialBreakdown` recalculation after room cancellation (totals, penalties, refunds) in `backend/src/test/java/com/acme/reservation/domain/FinancialBreakdownTest.java`
- [x] T037 [P] [US3] Contract test (Pact provider) for `DELETE /api/v1/reservations/{id}/rooms/{roomId}` 202 and 200 (idempotent) in `backend/src/test/java/com/acme/reservation/contract/CancelRoomContractTest.java`
- [x] T038 [P] [US3] Integration test for partial cancellation atomicity: room transitions, aggregate recalculation, history event in `backend/src/test/java/com/acme/reservation/CancelRoomIT.java`

### Implementation for User Story 3

- [x] T039 [US3] Implement `FinancialCalculationService` domain service for recalculating reservation totals, penalties, and refunds in `backend/src/main/java/com/acme/reservation/domain/financial/FinancialCalculationService.java`
- [x] T040 [US3] Implement `FinancialBreakdown` entity with line-item structure and currency consistency validation in `backend/src/main/java/com/acme/reservation/domain/financial/FinancialBreakdown.java`
- [x] T041 [US3] Implement `CancelRoomCommandHandler` with idempotency guard, room state validation, atomic aggregate update, and history event emission in `backend/src/main/java/com/acme/reservation/application/command/CancelRoomCommandHandler.java`
- [x] T042 [US3] Add `DELETE /api/v1/reservations/{reservationId}/rooms/{roomItemId}` to `ReservationController` (requires `X-Idempotency-Key`) in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/ReservationController.java`

**Checkpoint**: User Stories 1, 2, and 3 independently functional — partial cancellation with atomic update and history

---

## Phase 6: User Story 4 — Cancel Entire Reservation (Priority: P3)

**Goal**: Cancel all room items in a reservation idempotently, trigger asynchronous downstream processing, and maintain complete audit history.

**Independent Test**: Cancel the same reservation repeatedly; verify idempotent cancellation semantics, async processing acknowledgement, and audit trail completeness.

### Tests for User Story 4

- [x] T043 [P] [US4] Unit test for full cancellation idempotency: repeated cancellation calls do not alter finalized state in `backend/src/test/java/com/acme/reservation/domain/FullCancellationTest.java`
- [x] T044 [P] [US4] Unit test for `IntegrationTask` state machine (READY→RUNNING→SUCCEEDED and retry/DLQ paths) in `backend/src/test/java/com/acme/reservation/domain/IntegrationTaskStateTest.java`
- [x] T045 [P] [US4] Contract test (Pact provider) for `DELETE /api/v1/reservations/{reservationId}` 202 and 200 (idempotent) in `backend/src/test/java/com/acme/reservation/contract/CancelReservationContractTest.java`
- [x] T046 [P] [US4] Integration test for full cancellation idempotency: 3 repeated calls produce one audit event and one DLQ evaluation in `backend/src/test/java/com/acme/reservation/CancelReservationIT.java`
- [x] T047 [P] [US4] Integration test for concurrent optimistic lock conflict: two simultaneous cancellations produce HTTP 409 for one caller in `backend/src/test/java/com/acme/reservation/ConcurrentCancellationIT.java`

### Implementation for User Story 4

- [x] T048 [US4] Implement `CancelReservationCommandHandler` with idempotency guard, all-room state transition, and async `IntegrationTask` dispatch in `backend/src/main/java/com/acme/reservation/application/command/CancelReservationCommandHandler.java`
- [x] T049 [US4] Implement `IntegrationTaskProducer` outbound adapter enqueuing cancellation tasks to durable queue in `backend/src/main/java/com/acme/reservation/adapters/outbound/messaging/IntegrationTaskProducer.java`
- [x] T050 [US4] Add `DELETE /api/v1/reservations/{reservationId}` to `ReservationController` (requires `X-Idempotency-Key`) in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/ReservationController.java`

**Checkpoint**: All four user stories independently functional — full reservation lifecycle implemented

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Async retry worker, DLQ replay, end-to-end tests, and quality gate sign-off

- [x] T051 [P] Implement async worker consumer with exponential backoff (base 1s, max 60s, multiplier 2, jitter), transient/permanent failure classification, and circuit-breaker protection in `backend/src/main/java/com/acme/reservation/adapters/outbound/partner/PartnerIntegrationWorker.java`
- [x] T052 [P] Implement `DlqRouter` routing terminal failures to DLQ with masked payload reference and attempt history in `backend/src/main/java/com/acme/reservation/adapters/outbound/messaging/DlqRouter.java`
- [x] T053 [P] Implement `DlqController` with `POST /api/v1/operations/dlq/{dlqId}/replay` (admin scope required, reason mandatory, audit event emitted) in `backend/src/main/java/com/acme/reservation/adapters/inbound/http/reservation/DlqController.java`
- [x] T054 [P] E2E test for reservation lifecycle: create → retrieve → partial cancel → full cancel in `backend/src/test/java/com/acme/reservation/e2e/ReservationLifecycleE2ETest.java`
- [x] T055 [P] E2E security test for PII access-control enforcement, unauthorized PII access denial, and OWASP ASVS L3 scenarios in `backend/src/test/java/com/acme/reservation/e2e/ReservationSecurityE2ETest.java`
- [ ] T056 Verify SonarQube quality gate passes: zero Blocker/Critical/Major violations, aggregate/domain coverage ≥ 80%
- [x] T057 [P] Validate all endpoints in `contracts/reservation-api.yaml` are implemented and return correct HTTP status codes and RFC 9457 errors

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **blocks all user stories**
- **Phase 3 (US1 — P1)**: Requires Phase 2 complete — **MVP, deliver first**
- **Phase 4 (US2 — P2)**: Requires Phase 2 complete — can run in parallel with Phase 3
- **Phase 5 (US3 — P3)**: Requires Phase 2 and US1 domain entities complete — can run in parallel with Phase 4
- **Phase 6 (US4 — P3)**: Requires Phase 2 and US3 `CancelRoomCommandHandler` pattern established — can run in parallel with Phase 5
- **Phase 7 (Polish)**: Requires all user story phases complete

### User Story Dependencies

- **US1 (P1)**: Standalone — entry point for all lifecycle operations
- **US2 (P2)**: Reads US1-created data; query-side is independent of US1 implementation details
- **US3 (P3)**: Depends on US1 aggregate root; partial cancel extends Reservation domain
- **US4 (P3)**: Depends on US1 aggregate root; full cancel follows same command-handler pattern as US3

### Within Each User Story

- Tests written before implementation (fail first)
- Domain entities before application handlers
- Outbound port adapters before inbound HTTP controllers
- Command/query handlers before controllers

### Parallel Opportunities

- Phase 2: T010–T016 can run in parallel after T009 (`Reservation` aggregate) and T008 (enums)
- US1: T017–T020 (tests) in parallel; T022–T024 (JPA adapters) in parallel
- US2: T026–T029 (tests) in parallel; T030–T034 (impl) mostly sequential
- US3: T035–T038 (tests) in parallel; T039–T040 in parallel
- US4: T043–T047 (tests) in parallel

---

## Parallel Example: User Story 3 (Partial Cancellation)

```
T035 ──┐
T036 ──┤
T037 ──┼──► pass in parallel ──► T039 ──► T040 ──► T041 ──► T042
T038 ──┘
```

---

## Implementation Strategy

- **MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1) — delivers idempotent creation with async status progression
- **Incremental delivery**: US2 (retrieve + PII masking), US3 (room cancel), US4 (full cancel) as independent increments
- **Each task describes a single file** with enough context for LLM implementation without additional input

---

## Task Summary

| Phase | Tasks | User Story | Parallel Tasks |
|-------|-------|------------|----------------|
| Phase 1 — Setup | T001–T003 | — | T002, T003 |
| Phase 2 — Foundational | T004–T016 | — | T006, T007, T010–T016 |
| Phase 3 — US1 (P1) | T017–T025 | US1 | T017–T020 |
| Phase 4 — US2 (P2) | T026–T034 | US2 | T026–T029 |
| Phase 5 — US3 (P3) | T035–T042 | US3 | T035–T038, T039–T040 |
| Phase 6 — US4 (P3) | T043–T050 | US4 | T043–T047 |
| Phase 7 — Polish | T051–T057 | — | T051–T055, T057 |

**Total tasks**: 57  
**Total parallelizable**: 36  
**MVP subset (Phase 1–3)**: 25 tasks
