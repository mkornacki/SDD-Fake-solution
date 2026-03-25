Project Constitution – Java 11 + Spring Boot
Aligned with RFC 2119, Clean Architecture, Clean Code, OWASP ASVS Level 3, Kubernetes-ready, SonarQube-compliant
1. General Principles
•	The project MUST be deterministic, predictable, and fully testable.
•	Every class, component, and module MUST have a clear and explicit input/output contract.
•	Code MUST be readable, modular, and follow the Single Source of Truth principle.
•	The project MUST follow Clean Code, Clean Architecture, SOLID, DRY, KISS, and YAGNI principles.
•	The system MUST be secure by default and secure by design.
•	The system MUST comply with OWASP ASVS Level 3.
•	All code, documentation, comments, commit messages, ADRs, and repository content MUST be written and maintained in English.
•	The file .specify/memory/constitution.md is the single source of truth.
•	Execution guidelines in .github/agents/ and .specify/templates/ MUST remain aligned with the constitution after every change.
2. Architecture
•	The architecture MUST follow Clean Architecture and the principles of SOLID, CQRS, IoC, DI, DRY, KISS, and YAGNI.
•	The domain layer MUST NOT depend on Spring Boot, Spring Framework, web libraries, or ORM frameworks.
•	Domain modules MUST be isolated and communicate only through explicit interfaces.
•	The directory structure MUST reflect domain modules.
•	Logical monoliths MUST NOT be created.
•	The architecture MUST support OWASP ASVS Level 3 requirements.
•	The solution MUST be designed for deployment on a Kubernetes cluster, including:
o	12 factor app compliance,
o	externalized configuration (ConfigMap/Secret),
o	readiness/liveness probes,
o	statelessness (or explicitly separated stateful components),
o	structured logging to stdout.
•	The entire solution and all modules MUST follow Semantic Versioning 2.0.0.
3. Quality and Testing
Testing Pyramid
The project MUST follow the testing pyramid:
•	the majority of tests are unit tests,
•	fewer integration tests,
•	the fewest end to end tests,
•	contract tests MUST be part of the integration layer.
Determinism and Isolation
•	Tests MUST be deterministic.
•	Tests MUST be isolated — NO shared mutable state is allowed between tests.
•	Tests MUST run without network access, unless explicitly classified as integration or end to end tests.
Test Scenarios
•	Test scenarios MUST cover all business functions, edge cases, and error paths.
•	Scenarios MUST be unambiguous, repeatable, and aligned with domain and security requirements.
Unit and Integration Tests
•	Domain logic MUST have 100% unit test coverage.
•	Every public method MUST have a unit test.
•	Interactions between modules MUST be tested via integration tests.
•	Testcontainers MAY be used for integration, end to end, development, and CI/CD environments.
Contract Tests (Pact or equivalent)
•	The application MUST include contract tests for all API integrations.
•	Contract tests MUST use Pact or an equivalent tool.
•	Contracts MUST be versioned and stored in a repository or contract broker.
•	A build MUST NOT be merged if contract validation fails.
End to End Tests
•	End to end tests MUST cover all critical user flows.
•	End to end tests MUST be written using Playwright.
•	End to end tests MUST include security scenarios aligned with OWASP ASVS Level 3.
Quality & Security Scanning (SonarQube)
•	Results from unit, integration, contract, and end to end tests MUST be passed to SonarQube.
•	Code coverage MUST be reported to SonarQube.
•	Static analysis (SAST) MUST run on every build.
•	A build MUST NOT be merged unless the SonarQube quality gate passes.
•	Code in every language (Java, YAML, JSON, Dockerfile, Bash, JS/TS, etc.) MUST comply with SonarQube’s default rules for that language.
•	Violations of severity Blocker, Critical, or Major MUST NOT be accepted.
4. Code Standards
•	Code style MUST follow the selected standard (Checkstyle / Spotless / Google Java Style / Sonar ruleset).
•	Methods longer than 40 lines MUST NOT be accepted.
•	Every commit MUST follow Conventional Commits.
•	Code MUST be self-documenting.
•	Security mechanisms MUST NOT be accidentally bypassable.
•	Code MUST follow Clean Code and YAGNI principles.
•	All code, comments, documentation, and commit messages MUST be written in English.
•	Caching mechanisms MAY be used, provided they:
o	improve performance,
o	do not compromise data consistency,
o	are documented.
•	Any SonarQube rule suppression MUST be documented in an ADR.
5. Security
•	Input validation MUST occur at system boundaries.
•	Sensitive data MUST NOT be logged.
•	Error handling MUST be explicit and controlled.
•	APIs MUST be resistant to OWASP Top 10 vulnerabilities.
•	The system MUST follow secure-by-default and secure-by-design principles.
•	Dependencies MUST be kept up to date.
•	The system MUST comply with GDPR.
•	Every endpoint MUST require authentication.
•	Anonymous endpoints MUST NOT exist.
•	The solution SHOULD support OAuth 2.0 / OIDC authentication and authorization.
•	Swagger/OpenAPI documentation MUST NOT be exposed in production.
6. API and UX
•	API MUST return errors in RFC 9457 format.
•	API responses MUST be consistent and predictable.
•	UI MUST comply with WCAG 2.1 AA.
•	Personal data MUST be processed in accordance with GDPR.
•	The application MUST support Swagger / OpenAPI.
•	Every endpoint MUST be fully documented in OpenAPI.
API Performance Requirements
•	API endpoints MUST respond within ≤ 200 ms at p95 under normal load defined as 3 million requests per hour.
•	Any endpoint exceeding this threshold MUST have:
o	documented justification,
o	a remediation plan,
o	an architectural impact assessment.
7. Delivery Process
•	During the /clarify phase, the agent MUST ask questions whenever any requirement is unclear.
•	The implementation plan MUST comply with the constitution and specification.
•	Tasks MUST be small, unambiguous, and executable in a single step.
•	CI/CD MUST run all test types and SonarQube scanning.
•	Contract tests MUST be a mandatory CI/CD stage.
•	Every PR MUST pass the SonarQube quality gate.
•	Quality regressions MUST be fixed before deployment.
•	Docker, Docker Compose, and Testcontainers MAY be used in development and CI/CD.
•	All architectural decisions MUST be stored in the repository following ADR best practices.
•	The ADR template MUST be clear, readable, and easy to understand.
•	Technical decisions MUST be evaluated against all principles in this constitution.
•	Any decision violating the constitution MUST have a formal exception recorded in an ADR.
•	Implementation choices MUST explicitly state which principles they support and how compliance is ensured.
•	The delivery process MUST support Kubernetes deployment requirements.
•	All repository content (code, docs, ADRs, issues, PR descriptions) MUST be maintained in English.
8. Technologies
•	Java MUST be version 11.
•	The backend framework MUST be the latest stable Spring Boot version compatible with Java 11.
•	Spring libraries MUST follow the Spring Boot BOM.
•	ORM MUST be JPA with Hibernate.
•	The domain layer MUST NOT depend on ORM.
•	The application MUST be ready for Kubernetes deployment.
9. Database
•	SQLite MUST be used as the primary development/test database (or another explicitly approved alternative).
•	Database migrations MUST be versioned and repeatable.
•	Schema changes MUST NOT break domain contracts without appropriate data migration.
10. Constitution Change Rules
•	Changes MUST be introduced in a separate PR.
•	Every change MUST be justified and explicitly approved.


**Version**: [CONSTITUTION_VERSION] | **Ratified**: [RATIFICATION_DATE] | **Last Amended**: [LAST_AMENDED_DATE]
<!-- Example: Version: 2.1.1 | Ratified: 2025-06-13 | Last Amended: 2025-07-16 -->
