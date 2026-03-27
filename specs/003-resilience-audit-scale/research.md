# Research: Resilient Reservation Operations

## Decision 1: Idempotency keying and concurrency control
- Decision: Use a deterministic business fingerprint (normalized partner id + external request reference + reservation identity dimensions) as the idempotency key, persisted with a unique constraint and guarded by transactional upsert semantics.
- Rationale: This guarantees retry safety and duplicate prevention under concurrency while allowing the API to return the same canonical operation result for repeated requests.
- Alternatives considered: Client-generated opaque idempotency key only (rejected because key quality and reuse discipline vary by partner); in-memory deduplication (rejected because it fails across instances and restarts).

## Decision 2: Asynchronous partner processing model
- Decision: Accept reservation create synchronously for validation + enqueue, then process partner communication via a durable queue with worker consumers.
- Rationale: Decouples partner latency/outages from intake path and enables controlled retry, DLQ routing, and replay operations.
- Alternatives considered: Fully synchronous partner call in request path (rejected due to availability risk and poor tail latency); fire-and-forget without durable queue (rejected due to loss/recovery gaps).

## Decision 3: Retry and overload protection
- Decision: Apply bounded exponential backoff with jitter, retry classification (transient vs permanent), max-attempt caps, and circuit-breaker/open-state throttling to prevent retry storms.
- Rationale: Improves recovery of transient failures while protecting upstream/downstream systems during outages.
- Alternatives considered: Fixed-interval retry (rejected because it amplifies coordinated retries); unlimited retries (rejected due to queue saturation and operational risk).

## Decision 4: Dead-letter and replay strategy
- Decision: Route terminal failures to DLQ with immutable failure envelope containing correlation id, partner endpoint, masked payload pointers, attempt history, and classification; support operator-triggered replay with idempotency preservation.
- Rationale: Preserves forensic traceability and enables safe recovery without duplicate side effects.
- Alternatives considered: Drop failed messages after max retries (rejected due to audit/compliance and recovery gaps); auto-replay all DLQ items (rejected due to risk of repeated harmful failures).

## Decision 5: Audit and sensitive data handling
- Decision: Emit immutable audit events for state transitions and security-relevant actions, store partner request/response snapshots with field-level masking/tokenization for sensitive attributes, and enforce redaction in logs/exports.
- Rationale: Satisfies compliance, governance, and operational investigation requirements while minimizing exposure.
- Alternatives considered: Log-only auditing (rejected due to incompleteness and tamper concerns); full raw payload visibility to operators (rejected due to privacy/security policy violations).

## Decision 6: Retention governance
- Decision: Apply tiered retention rules by artifact class: short-lived raw payload snapshots, longer-lived audit metadata, and policy-based legal hold exceptions.
- Rationale: Balances GDPR-like minimization obligations with traceability requirements.
- Alternatives considered: Single retention window for all records (rejected as over-retaining some data and under-retaining governance-critical metadata).

## Decision 7: Technical stack alignment
- Decision: Implement as Java 11 + latest stable Spring Boot compatible with Java 11, Spring Data JPA/Hibernate, SQLite for dev/test, queue abstraction suitable for Kubernetes deployment, and OpenAPI-documented HTTP API.
- Rationale: Aligns with constitution mandates and provides robust ecosystem support for resilience patterns.
- Alternatives considered: Non-Java stack (rejected by constitution); direct ORM dependency in domain layer (rejected by Clean Architecture constraints).

## Decision 8: Testing and quality enforcement
- Decision: Enforce testing pyramid with domain-focused unit tests, integration tests for persistence/queue boundaries, contract tests for partner APIs (Pact or equivalent), and Playwright-based end-to-end coverage for critical flows and security scenarios.
- Rationale: Meets constitution quality gates and catches failures at appropriate layers.
- Alternatives considered: Integration-heavy only strategy (rejected due to slower, less deterministic feedback); unit-only strategy (rejected because external contract and workflow risks remain unverified).

## Decision 9: Operational observability and SLO instrumentation
- Decision: Publish metrics for request rate, p95/p99 latency, queue depth, consumer lag, retry counts, DLQ growth, idempotency-hit ratio, and error classes; include trace correlation across API and worker.
- Rationale: Supports SC-002, SC-003, SC-006 and rapid incident response under load.
- Alternatives considered: Basic uptime-only monitoring (rejected due to poor diagnosability).
