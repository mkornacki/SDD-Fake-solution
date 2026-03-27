# Implementation Plan: Backend Foundation REST API

**Branch**: `001-backend-foundation` | **Date**: 2026-03-25 | **Spec**: `/workspaces/Hrs_assessment/specs/001-backend-foundation/spec.md`
**Input**: Feature specification from `/specs/001-backend-foundation/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Establish a production-grade Java 11 + Spring Boot backend foundation that exposes only REST endpoints returning JSON responses. The foundation enforces authenticated access on every endpoint, produces RFC 9457 error responses, emits structured logs to stdout, and exposes Kubernetes-compatible health probes. It defines versioned API contracts, Clean Architecture module boundaries, and comprehensive quality/security gates that all subsequent features inherit.

## Technical Context

**Language/Version**: Java 11  
**Primary Dependencies**: Spring Boot (latest stable compatible with Java 11), Spring Web MVC, Spring Security (OAuth 2.0 / OIDC Resource Server), Spring Data JPA, Hibernate, Liquibase (schema migrations), OpenAPI/Swagger (springdoc-openapi), Pact (contract tests), SonarQube quality gate integration  
**Storage**: SQLite for development/test via JPA/Hibernate; schema managed by Liquibase migrations  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (integration), Pact (contract), Playwright (end-to-end and OWASP ASVS security scenarios), SonarQube  
**Target Platform**: Linux container on Kubernetes (12-factor, readiness/liveness probes, stdout structured logging, externalized configuration via ConfigMap/Secret)  
**Project Type**: Backend web service (REST API only, no frontend/UI rendering)  
**Performance Goals**: p95 response time <= 200 ms under normal load (3 million requests/hour)  
**Constraints**: Clean Architecture isolation (domain layer free of Spring/ORM dependencies), OWASP ASVS Level 3, GDPR-compliant logging, RFC 9457 error format, OpenAPI docs disabled in production, authenticated-only endpoints, Semantic Versioning 2.0.0  
**Scale/Scope**: Foundation for all future domain modules; must support horizontal scaling; 3M req/h normal load baseline

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Gate 1 - Language and framework compliance: PASS. Java 11 + Spring Boot ecosystem as mandated.
- Gate 2 - Clean Architecture boundaries: PASS. Domain layer defined independently of Spring/ORM; adapters isolate HTTP, persistence, and security.
- Gate 3 - Security baseline (OWASP ASVS L3 / GDPR / auth): PASS with implementation obligations. Every endpoint requires authentication, no anonymous access, RFC 9457 error model, structured logs without PII, OpenAPI disabled in production.
- Gate 4 - Testing and quality gates: PASS with required deliverables. Testing pyramid (unit/integration/contract/E2E), SonarQube gate, Pact contract tests, Playwright E2E including security scenarios.
- Gate 5 - API standards: PASS. OpenAPI/Swagger enabled in non-production, RFC 9457 errors, consistent response envelopes, versioned contracts.
- Gate 6 - Kubernetes readiness: PASS. Stateless, 12-factor, externalized config, readiness/liveness health probes, structured stdout logging.

**Post-design re-check**: No constitutional violations. No exception ADR required.

## Project Structure

### Documentation (this feature)

```text
specs/001-backend-foundation/
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
├── src/main/java/com/acme/foundation/
│   ├── domain/
│   │   ├── health/
│   │   └── audit/
│   ├── application/
│   │   ├── ports/inbound/
│   │   └── ports/outbound/
│   ├── adapters/
│   │   ├── inbound/http/
│   │   │   ├── health/
│   │   │   ├── error/
│   │   │   └── security/
│   │   └── outbound/persistence/
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

**Structure Decision**: Backend-only Clean Architecture layout with explicit domain/application/adapter separation. No frontend components. OpenAPI documentation served only in non-production Spring profiles.

## Complexity Tracking

> No constitution violations requiring formal exception at planning stage.
