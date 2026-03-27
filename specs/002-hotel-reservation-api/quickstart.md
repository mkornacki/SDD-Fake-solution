# Quickstart: Hotel Reservation Partner API

## Purpose
Validate idempotent multi-room reservation creation, partial and full cancellation, reservation retrieval with change history, asynchronous partner processing with retry and DLQ handling, PII masking, and financial aggregate consistency.

## Prerequisites
- Java 11
- Spring Boot application runtime with `local` profile
- Local SQLite database (configured via `application-local.yml`)
- Local queue emulator or message broker profile enabled for async workers
- Valid JWT token from local/dev OIDC issuer with appropriate scopes
- Liquibase migrations applied automatically on startup

## Run Locally
1. Navigate to `backend/` directory.
2. Start the service: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
3. Confirm readiness: `curl http://localhost:8080/actuator/health/readiness`
4. Obtain a JWT token and set `TOKEN=<your-jwt>` in your shell.

## Scenario 1: Create a multi-room reservation (idempotent)
1. Send `POST /api/v1/reservations` with `X-Idempotency-Key: key-001`, `Authorization: Bearer $TOKEN`, and a body containing `partnerId`, `guest`, and 2+ `rooms`.
2. Verify HTTP 202 Accepted with `reservationId` and `status: PENDING`.
3. Repeat the same request (same idempotency key and payload) 3+ times.
4. Verify no duplicate reservation is created; all responses return the same `reservationId`.
5. Poll `GET /api/v1/reservations/{reservationId}` until `status` transitions to `ACTIVE`.

## Scenario 2: Retrieve reservation with history
1. Call `GET /api/v1/reservations/{reservationId}` with a valid JWT that has PII access scope.
2. Verify HTTP 200 with full reservation details: rooms, financial breakdown, and `history` array with at least one CREATED event.
3. Repeat the call with a JWT that lacks PII access scope.
4. Verify sensitive fields (`guest.email`, `guest.phone`) are masked in the response.

## Scenario 3: Partial cancellation (single room)
1. With a reservation containing 2+ active rooms, call `DELETE /api/v1/reservations/{reservationId}/rooms/{roomItemId}` with `X-Idempotency-Key: cancel-room-001`.
2. Verify HTTP 202 Accepted.
3. Poll `GET /api/v1/reservations/{reservationId}` and verify:
   - The targeted room transitions to `CANCELLATION_PENDING`, then `CANCELLED`.
   - Other rooms remain `ACTIVE`.
   - Reservation `status` transitions to `PARTIALLY_CANCELLED`.
   - Financial totals are recalculated atomically.

## Scenario 4: Full reservation cancellation (idempotent)
1. Call `DELETE /api/v1/reservations/{reservationId}` with `X-Idempotency-Key: cancel-full-001`.
2. Verify HTTP 202 Accepted.
3. Repeat the same cancellation request 3 times.
4. Verify no additional side effects; reservation reaches `CANCELLED` status once.
5. Verify history contains a RESERVATION_CANCELLED event.

## Scenario 5: Asynchronous retry and DLQ routing
1. Configure the partner adapter stub to return transient HTTP 503 responses.
2. Submit a create reservation request.
3. Observe `IntegrationTask` state progression: `READY → RUNNING → RETRY_WAIT → RUNNING...`.
4. Verify retries use exponential backoff intervals.
5. Force terminal failure by exhausting max attempts.
6. Verify a `DLQItem` is created with `replayStatus: PENDING` and full failure context.

## Scenario 6: DLQ replay
1. Configure the partner adapter stub to succeed.
2. Call `POST /api/v1/operations/dlq/{dlqId}/replay` with operator JWT (admin scope) and `{ "reason": "Partner recovered from outage." }`.
3. Verify HTTP 202 Accepted.
4. Verify the reservation eventually reaches terminal `ACTIVE` or fulfilled state.
5. Verify `DLQItem.replayStatus` becomes `REPLAYED`.
6. Verify a history event captures operator identity and replay reason.

## Scenario 7: Concurrent cancellation safety
1. Send two simultaneous `DELETE /api/v1/reservations/{reservationId}/rooms/{roomItemId}` requests with different idempotency keys.
2. Verify exactly one is processed; the other returns HTTP 409 Conflict (optimistic lock) or 200 OK with the already-cancelled state.
3. Verify no duplicate or inconsistent history events.

## Test Strategy Mapping
- Unit tests: Reservation aggregate invariants, state transitions, financial recalculation, idempotency guard logic.
- Integration tests: JPA transaction atomicity, queue consumer behavior, Testcontainers for persistence.
- Contract tests: Partner API consumer/provider contracts using Pact.
- End-to-end tests: Critical user flows (create, retrieve, partial/full cancel) and security scenarios using Playwright.
