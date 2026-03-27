# Data Model: Backend Foundation REST API

## Entity: ApiEndpointContract
- Description: Versioned definition of an HTTP endpoint including route, method, request/response schemas, and available error model. Used as a design-time artifact; persisted as OpenAPI specification rather than a database entity.
- Fields:
  - `endpointId` (string, internal identifier)
  - `path` (string, required, e.g., `/api/v1/health`)
  - `method` (enum: GET, POST, PUT, PATCH, DELETE)
  - `apiVersion` (string, required, e.g., `v1`)
  - `requestSchema` (object reference, nullable for GET/DELETE)
  - `responseSchema` (object reference, required)
  - `errorModel` (object reference, RFC 9457 problem details)
  - `authRequired` (boolean, always `true`)
  - `documentedIn` (string: OpenAPI file path)
- Validation Rules:
  - `authRequired` MUST always be `true`; no anonymous endpoints.
  - Every endpoint MUST reference a valid `errorModel`.
  - `apiVersion` MUST follow Semantic Versioning prefix convention.

## Entity: ErrorDetail
- Description: Standardized problem response issued for all API error scenarios, conforming to RFC 9457 (Problem Details for HTTP APIs).
- Fields:
  - `type` (URI, required, problem type identifier)
  - `title` (string, required, human-readable summary)
  - `status` (integer, required, HTTP status code)
  - `detail` (string, optional, instance-specific explanation)
  - `instance` (URI, optional, request-specific identifier)
  - `extensions` (map of additional fields, optional)
- Validation Rules:
  - `type` MUST be a valid URI; MUST NOT expose internal stack traces.
  - `status` MUST match the HTTP response status code.
  - `detail` MUST NOT contain sensitive or personal data.
- Content Type: `application/problem+json`

## Entity: AuthenticationContext
- Description: Runtime representation of a validated caller identity and associated authorization scopes, derived from the JWT bearer token. Not persisted; used within the request processing pipeline.
- Fields:
  - `subjectId` (string, required, unique caller identity from JWT `sub` claim)
  - `scopes` (string list, required, from JWT `scope` or `authorities` claim)
  - `issuer` (string, required, JWT `iss` claim)
  - `tokenExpiry` (timestamp, required, JWT `exp` claim)
  - `clientId` (string, optional, OAuth 2.0 client identifier)
- Validation Rules:
  - Token MUST be validated against the configured OIDC issuer.
  - Expired tokens MUST be rejected with HTTP 401.
  - Missing or malformed tokens MUST be rejected with HTTP 401 before any business logic executes.

## Entity: HealthStatus
- Description: Machine-readable representation of service liveness and readiness state used by Kubernetes orchestration health checks.
- Fields:
  - `status` (enum: UP, DOWN, OUT_OF_SERVICE)
  - `components` (map: component name → ComponentHealth)
  - `checkedAt` (timestamp)
- Sub-entity: `ComponentHealth`
  - `status` (enum: UP, DOWN)
  - `details` (map of key-value metadata, optional)
- Validation Rules:
  - Health endpoints (`/actuator/health/liveness`, `/actuator/health/readiness`) MUST be accessible without authentication.
  - `status` = DOWN causes Kubernetes to restart (liveness) or stop routing traffic (readiness).
  - `details` MUST NOT expose sensitive configuration values.

## Entity: AuditEvent
- Description: Security-relevant activity record capturing non-sensitive metadata for traceability and compliance. Append-only; never mutated after creation.
- Fields:
  - `auditEventId` (UUID, required)
  - `actorId` (string, required, from AuthenticationContext.subjectId)
  - `actorType` (enum: USER, SERVICE, SYSTEM)
  - `action` (string, required, e.g., `ENDPOINT_ACCESS`, `AUTH_FAILURE`)
  - `resourceType` (string, optional)
  - `resourceId` (string, optional)
  - `outcome` (enum: SUCCESS, FAILURE)
  - `traceId` (string, required)
  - `occurredAt` (timestamp, required)
  - `ipAddressHash` (string, optional, hashed for privacy)
- Validation Rules:
  - `actorId`, `action`, `outcome`, `traceId`, and `occurredAt` are mandatory.
  - Events are append-only; no update or delete operations.
  - `ipAddressHash` MUST be a one-way hash; raw IP addresses MUST NOT be stored to comply with GDPR.

## API Response Envelope
- Description: Standard wrapper for successful JSON responses across all business endpoints.
- Fields:
  - `data` (object or array, required: the primary response payload)
  - `meta` (object, optional: pagination, version, timestamp)
- Validation Rules:
  - Error responses MUST use `ErrorDetail` (RFC 9457), not this envelope.
  - All successful responses MUST include `data`.

## State Notes
- No complex state machines at the foundation layer; lifecycle management belongs to domain modules built on this foundation.
- Health status transitions are managed by Spring Boot Actuator and registered `HealthIndicator` beans.
- AuditEvents are immutable from the moment of creation.
