# Quickstart: Resilient Reservation Operations

## Purpose
Validate idempotent create behavior, asynchronous partner processing, dead-letter replay readiness, and audit-safe observability.

## Prerequisites
- Java 11
- Spring Boot application runtime
- Local SQLite database for development/test
- Queue emulator or local message broker profile enabled

## Run Locally
1. Start the service in local profile.
2. Ensure schema migrations are applied.
3. Confirm readiness endpoint is healthy.

## Scenario 1: Idempotent create and retry safety
1. Send `POST /api/v1/reservations` with fixed `X-Idempotency-Key` and valid payload.
2. Repeat same request 5+ times, including concurrent submissions.
3. Verify one canonical reservation outcome and consistent status/result mapping.

## Scenario 2: Asynchronous retries and DLQ routing
1. Configure partner adapter to return transient errors.
2. Submit create request and observe status progression.
3. Verify retry schedule uses progressive delay with jitter.
4. Force terminal failure and verify DLQ entry includes correlation and attempt metadata.

## Scenario 3: Replay workflow
1. Invoke replay endpoint for a DLQ item.
2. Verify replay enqueues work and preserves original idempotency semantics.
3. Confirm audit events capture operator identity and replay action.

## Scenario 4: Audit and masking
1. Trigger create + failure path.
2. Review interaction logs/exports.
3. Confirm sensitive fields are masked and trace identifiers are present.

## Scenario 5: Observability and load
1. Execute load with retry bursts.
2. Validate metrics: latency, error rate, queue depth, lag, retry count, idempotency-hit rate.
3. Confirm critical reservation intake remains available under stress.

## Test Strategy Mapping
- Unit tests: domain idempotency and state-transition rules.
- Integration tests: persistence uniqueness, queue consumer behavior, retry classification.
- Contract tests: partner API using Pact or equivalent.
- End-to-end tests: critical user journeys and security controls.
