# Quickstart: Backend Foundation REST API

## Purpose
Validate that the backend foundation exposes authenticated REST endpoints returning JSON, enforces RFC 9457 error responses, provides Kubernetes-compatible health probes, and produces structured logs to stdout.

## Prerequisites
- Java 11
- Spring Boot application runtime
- Local SQLite database (configured via `application-local.yml`)
- A valid JWT issuer configured (local: mock OIDC server or dev-issued token)
- Liquibase migrations applied (automatic on startup)

## Run Locally
1. Clone the repository and navigate to the `backend/` directory.
2. Start the service: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
3. Confirm readiness: `curl http://localhost:8080/actuator/health/readiness`
4. Confirm liveness: `curl http://localhost:8080/actuator/health/liveness`
5. Open API documentation: `http://localhost:8080/swagger-ui.html`

## Scenario 1: Authenticated access to a REST endpoint
1. Obtain a valid JWT token from the configured OIDC issuer.
2. Call an authenticated endpoint: `GET /api/v1/health` with `Authorization: Bearer <token>`.
3. Verify HTTP 200 with a JSON body matching the `ApiEndpointContract` response schema.
4. Verify Content-Type header is `application/json`.

## Scenario 2: Unauthenticated request is rejected
1. Call any endpoint without an Authorization header: `GET /api/v1/health`.
2. Verify HTTP 401 response.
3. Verify response body conforms to RFC 9457 (`application/problem+json`) with `type`, `title`, and `status` fields.
4. Verify no business data is present in the response.

## Scenario 3: Invalid input returns controlled error
1. Send a `POST` request to a business endpoint with a malformed or missing required field.
2. Verify HTTP 422 (or 400) response.
3. Verify response body is RFC 9457 problem detail JSON.
4. Verify the error `detail` contains no stack traces or internal system information.

## Scenario 4: Health probes
1. Call `GET /actuator/health/liveness` without authentication.
2. Verify HTTP 200 with `{ "status": "UP" }`.
3. Call `GET /actuator/health/readiness` without authentication.
4. Verify HTTP 200 with `{ "status": "UP" }`.

## Scenario 5: API documentation availability
1. With `local` or `non-prod` profile active, access `http://localhost:8080/swagger-ui.html`.
2. Verify all endpoints are listed and documented with request/response schemas.
3. Switch to `prod` profile and verify `http://localhost:8080/swagger-ui.html` returns HTTP 404.

## Scenario 6: Structured logging validation
1. Trigger an endpoint call and observe stdout logs.
2. Verify each log entry is valid JSON with `level`, `message`, `traceId`, and `requestId` fields.
3. Verify no personally identifiable information or sensitive credential values appear in any log line.

## Test Strategy Mapping
- Unit tests: domain validation rules, authentication context parsing, error model construction.
- Integration tests: Spring Security filter chain, persistence migrations, Actuator health indicator composition.
- Contract tests: HTTP consumer/provider contracts for all published endpoints using Pact.
- End-to-end tests: critical user flows and OWASP ASVS L3 security scenarios using Playwright.
