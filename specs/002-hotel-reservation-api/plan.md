# Implementation Plan: Hotel Reservation Partner API

**Branch**: `002-hotel-reservation-api` | **Date**: 2026-03-25 | **Spec**: `/workspaces/Hrs_assessment/specs/002-hotel-reservation-api/spec.md`
**Input**: Feature specification from `/specs/002-hotel-reservation-api/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Build a production-grade RESTful API for business partner hotel reservation management. Capabilities include idempotent multi-room reservation creation, full and partial cancellations, reservation retrieval with change history, asynchronous partner integrations with retry and DLQ handling, PII protection, financial consistency with atomically updated aggregates, immutable audit trails, and horizontal scalability. Built on the foundation established in spec 001, using Java 11 + Spring Boot with Clean Architecture, JPA/Hibernate, durable queue integration, strict masking/retention controls, and constitution-compliant quality gates.

## Technical Context

**Language/Version**: Java 11  
**Primary Dependencies**: Spring Boot (latest stable compatible with Java 11), Spring Web MVC, Spring Security (OAuth 2.0 / OIDC Resource Server), Spring Data JPA, Hibernate, Liquibase, springdoc-openapi, Spring Retry / Resilience4j (exponential backoff, circuit breaker), durable queue abstraction, Pact (contract tests), SonarQube  
**Storage**: SQLite for development/test via JPA/Hibernate; durable message queue/broker abstraction for async processing and DLQ  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (integration), Pact (contract for partner APIs), Playwright (end-to-end and security scenarios), SonarQube quality gate  
**Target Platform**: Linux container on Kubernetes (12-factor, readiness/liveness probes, externalized config, structured stdout logging)  
**Project Type**: Backend web service with asynchronous worker components  
**Performance Goals**: API p95 <= 200 ms under normal load (3M req/h); asynchronous integration tasks: >= 99.9% reach terminal state within agreed SLA; >= 99.5% availability at peak load  
**Constraints**: Clean Architecture isolation (domain independent of Spring/ORM), OWASP ASVS Level 3, GDPR-compliant PII handling, idempotent create and cancel operations, RFC 9457 errors, atomic aggregate updates, immutable audit trail  
**Scale/Scope**: High-volume partner reservation system; concurrent multi-room bookings; partial/full cancellation lifecycle; asynchronous partner integration with DLQ replay

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Gate 1 - Language and framework compliance: PASS. Java 11 + Spring Boot ecosystem as mandated.
- Gate 2 - Clean Architecture boundaries: PASS. Domain aggregates (Reservation, RoomReservationItem) have no dependency on Spring or ORM; adapters isolate HTTP, persistence, messaging, and partner clients.
- Gate 3 - Security baseline (OWASP ASVS L3 / GDPR / auth): PASS with implementation obligations. All endpoints authenticated, PII encrypted/tokenized at rest, masked in logs and responses, immutable audit trail, RFC 9457 errors, no anonymous access.
- Gate 4 - Testing and quality gates: PASS with required deliverables. Testing pyramid enforced, SonarQube gate mandatory, Pact contract tests for partner APIs, Playwright E2E and security flows.
- Gate 5 - API standards: PASS. OpenAPI/Swagger enabled in non-production only, RFC 9457 errors, consistent response envelope, versioned contracts.
- Gate 6 - Kubernetes readiness: PASS. Stateless API + async worker design, externalized config, health probes, structured stdout logs.
- Gate 7 - Financial consistency: PASS with implementation obligations. Aggregate updates (Reservation totals, RoomReservationItem status) are atomic within transactional boundaries; idempotency prevents double-processing.

**Post-design re-check**: No constitutional violations. No exception ADR required.

## Project Structure

### Documentation (this feature)

```text
specs/002-hotel-reservation-api/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/acme/reservation/
│   ├── domain/
│   │   ├── reservation/
│   │   │   ├── Reservation.java
│   │   │   ├── RoomReservationItem.java
│   │   │   ├── ReservationStatus.java
│   │   │   └── RoomStatus.java
│   │   ├── cancellation/
│   │   ├── financial/
│   │   ├── idempotency/
│   │   └── audit/
│   ├── application/
│   │   ├── command/
│   │   │   ├── CreateReservationCommand.java
│   │   │   ├── CancelReservationCommand.java
│   │   │   └── CancelRoomCommand.java
│   │   ├── query/
│   │   │   └── GetReservationQuery.java
│   │   └── ports/
│   │       ├── inbound/
│   │       └── outbound/
│   ├── adapters/
│   │   ├── inbound/http/
│   │   │   ├── reservation/
│   │   │   └── error/
│   │   ├── outbound/persistence/
│   │   ├── outbound/messaging/
│   │   └── outbound/partner/
│   └── configuration/
├── src/main/resources/
│   ├── db/migration/
│   └── application*.yml
└── src/test/
    ├── unit/
    ├── integration/
    ├── contract/
    └── e2e/
```

**Structure Decision**: Backend-only Clean Architecture layout extending the foundation (spec 001) with domain aggregates for reservation lifecycle, async processing adapters, and financial consistency mechanisms. CQRS applied at the application layer (commands vs. queries separated).

## Complexity Tracking

> No constitution violations requiring formal exception at planning stage.
