# Research: Hotel Reservation Partner API

## Decision 1: Reservation aggregate design (CQRS + DDD)
- Decision: Model Reservation as a DDD aggregate root owning a collection of RoomReservationItems. Apply CQRS to separate write (command) and read (query) operations at the application layer, with separate command handlers and query handlers.
- Rationale: Reservation lifecycle comprises complex state transitions, financial consistency requirements, and partial cancellation semantics that are best controlled through an aggregate root with explicit invariant enforcement. CQRS provides clean boundaries for read-optimized views (history, financial breakdown) vs. write-optimized command processing.
- Alternatives considered: Transaction script / service-only approach (rejected due to poor invariant enforcement for concurrent cancellation); separate microservice per operation (rejected because constitution prohibits logical monolith AND mandates module isolation within a single deployment).

## Decision 2: Idempotency for create and cancel operations
- Decision: Require a client-provided `X-Idempotency-Key` header on create and cancellation requests. Persist an `IdempotencyRecord` with a unique constraint on the key, storing the canonical operation result. Transactional upsert with database-level uniqueness prevents duplicate processing under concurrent retries.
- Rationale: FR-003 and FR-008 mandate idempotent create and cancel. Client-provided keys allow callers to deterministically construct retry-safe keys based on their business intent.
- Alternatives considered: Server-generated deduplication keys (rejected because clients cannot reconstruct them for retries); in-memory deduplication (rejected because it fails across instances and restarts).

## Decision 3: Asynchronous partner integration
- Decision: Accept reservation create and cancellation requests synchronously for validation, then enqueue an `IntegrationTask` for asynchronous partner processing via a durable queue. The API returns 202 Accepted immediately. Callers poll or receive callbacks for final status.
- Rationale: FR-005 requires asynchronous integrations with retry and DLQ. Partner latency/outages must not affect reservation intake availability.
- Alternatives considered: Synchronous partner call in request path (rejected due to availability coupling and latency risk); fire-and-forget without durability (rejected because lost messages cannot be recovered or DLQ-routed).

## Decision 4: Retry strategy and DLQ
- Decision: Apply bounded exponential backoff with jitter (base 1s, max 60s, multiplier 2), classify failures as transient vs. permanent, cap retry attempts per configurable policy, protect with circuit-breaker-style throttling during partner outages. Route terminal failures to DLQ with full attempt history and masked payload reference.
- Rationale: FR-016 requires retry with DLQ. Exponential backoff with jitter prevents coordinated retry storms. Circuit-breaker protects partner from overload after recovery.
- Alternatives considered: Fixed-interval retry (rejected because it generates coordinated storm on recovery); unlimited retries (rejected due to queue saturation risk).

## Decision 5: Atomic financial consistency
- Decision: All state changes to `Reservation` (totals, cancellation fees, refunds) and `RoomReservationItem` (room status, price recalculation) are applied within a single database transaction. Aggregate recalculates derived financial fields after every state transition.
- Rationale: FR-009 and FR-010 require atomic updates and financial consistency across price breakdowns, penalties, and refunds. Database transactions enforce all-or-nothing semantics.
- Alternatives considered: Eventual consistency for financial aggregates (rejected because FR-010 explicitly requires consistency across partial cancellation recalculations); separate financial microservice (rejected by deployment scope).

## Decision 6: PII protection strategy
- Decision: Encrypt/tokenize PII fields (guest name, email, phone) at rest using field-level encryption with a managed key. Apply column-level masking in API responses based on caller's PII authorization scope from the JWT. Logs never contain raw PII values.
- Rationale: FR-013 mandates PII protection: encryption or tokenization at rest, masking in logs/responses when permission is absent. GDPR data minimization principle applies.
- Alternatives considered: Full payload encryption (rejected due to operational query difficulty); no field-level encryption (rejected because it violates GDPR and FR-013).

## Decision 7: Immutable audit trail
- Decision: Emit a `ReservationHistoryEvent` (immutable append-only entity) for every state-changing operation: create, cancellation (full/partial), status transitions, financial recalculations, and error events. Include actor identity, timestamp, action, reason, and before/after aggregate state references.
- Rationale: FR-012 requires immutable audit trail with actor, timestamp, action, reason, and outcome.
- Alternatives considered: Mutable history record (rejected because it can be tampered with); log-only audit (rejected due to incomplete traceability and purge risk).

## Decision 8: Concurrent modification safety
- Decision: Apply optimistic locking (JPA `@Version`) on the `Reservation` aggregate. Concurrent conflicting modifications result in an `OptimisticLockException` which is mapped to HTTP 409 Conflict for the caller to retry.
- Rationale: FR-011 requires safe concurrent modification handling to prevent race-condition-induced inconsistent states. Optimistic locking is lighter than pessimistic locking and compatible with horizontal scaling.
- Alternatives considered: Pessimistic database locks (rejected due to scalability concerns and deadlock risk at high concurrency); no concurrency protection (rejected because it violates FR-011).

## Decision 9: Partial cancellation semantics
- Decision: Partial cancellation targets a specific `RoomReservationItem` by ID within a `Reservation`. The command validates that the room is in a cancellable state, transitions only that room, atomically recalculates reservation-level aggregates, emits history events, and enqueues asynchronous downstream processing.
- Rationale: FR-007 requires room-level partial cancellation as an independent operation. Only the targeted room must change state; other rooms must remain unaffected.
- Alternatives considered: Cancel-then-recreate pattern (rejected because it is not idempotent and introduces availability gaps); full-reservation re-evaluation on partial cancel (rejected as it violates the independence of room items).

## Decision 10: Testing strategy
- Decision: Unit tests cover all domain aggregate invariants, state machine transitions, financial recalculation logic, and idempotency rules. Integration tests validate persistence boundaries, transaction atomicity, and queue consumer behavior. Pact contract tests for partner API interactions. Playwright end-to-end and security tests for critical user flows.
- Rationale: Constitution mandates all four test layers, SonarQube gate, isolated deterministic tests.
- Alternatives considered: Integration-heavy strategy without domain unit tests (rejected because domain invariants need fast isolated verification).
