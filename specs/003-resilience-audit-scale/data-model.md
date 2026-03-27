# Data Model: Resilient Reservation Operations

## Entity: ReservationRequestContext
- Description: Canonical representation of reservation creation intent and lifecycle state.
- Fields:
  - `contextId` (UUID, immutable)
  - `partnerId` (string, required)
  - `businessFingerprint` (string, required, unique with partner scope)
  - `clientRequestId` (string, optional)
  - `status` (enum: RECEIVED, VALIDATED, QUEUED, PROCESSING, COMPLETED, FAILED, DLQ)
  - `canonicalReservationId` (string, nullable until COMPLETED)
  - `createdAt` (timestamp, required)
  - `updatedAt` (timestamp, required)
  - `traceId` (string, required)
- Validation Rules:
  - `businessFingerprint` MUST be deterministic for semantically identical requests.
  - `partnerId` + `businessFingerprint` MUST be unique.
- Relationships:
  - One-to-one with `IdempotencyRecord`.
  - One-to-many with `AsynchronousWorkItem` (historical attempts).

## Entity: IdempotencyRecord
- Description: Persistence guard ensuring retried creates map to one canonical outcome.
- Fields:
  - `idempotencyKey` (string, primary key)
  - `firstSeenAt` (timestamp, required)
  - `lifecycleState` (enum: IN_PROGRESS, COMPLETED, FAILED)
  - `resultReference` (string, nullable)
  - `responseDigest` (string, nullable)
  - `expiresAt` (timestamp, policy-driven)
- Validation Rules:
  - Single active record per key.
  - Transition to COMPLETED requires non-null `resultReference`.

## Entity: AsynchronousWorkItem
- Description: Durable unit for partner communication processing.
- Fields:
  - `workItemId` (UUID, immutable)
  - `contextId` (UUID, required)
  - `scheduledAt` (timestamp, required)
  - `attemptCount` (integer, default 0)
  - `maxAttempts` (integer, required)
  - `nextAttemptAt` (timestamp, nullable)
  - `state` (enum: READY, RUNNING, RETRY_WAIT, SUCCEEDED, TERMINAL_FAILED)
  - `failureClass` (enum: TRANSIENT, PERMANENT, NONE)
- Validation Rules:
  - `attemptCount <= maxAttempts`.
  - `nextAttemptAt` required when `state=RETRY_WAIT`.
- Relationships:
  - Many-to-one to `ReservationRequestContext`.
  - One-to-many with `PartnerInteractionLog`.

## Entity: PartnerInteractionLog
- Description: Audit-safe request/response trace for partner calls.
- Fields:
  - `interactionId` (UUID)
  - `workItemId` (UUID, required)
  - `requestSnapshotRef` (string, required)
  - `responseSnapshotRef` (string, nullable)
  - `httpStatus` (integer, nullable)
  - `maskedFieldsApplied` (string list, required)
  - `errorCode` (string, nullable)
  - `recordedAt` (timestamp, required)
- Validation Rules:
  - Stored snapshots MUST have masking policy version metadata.
  - Direct storage of unmasked sensitive fields in operator-visible records is forbidden.

## Entity: DeadLetterItem
- Description: Terminally failed operation preserved for replay and reconciliation.
- Fields:
  - `dlqId` (UUID)
  - `contextId` (UUID, required)
  - `failureReason` (string, required)
  - `attemptHistoryRef` (string, required)
  - `replayStatus` (enum: PENDING, IN_REVIEW, REPLAYED, CLOSED)
  - `replayCount` (integer, default 0)
  - `createdAt` (timestamp, required)
- Validation Rules:
  - Replay action MUST preserve original idempotency key semantics.
  - Replay is allowed only for authorized actors.

## Entity: AuditEvent
- Description: Immutable compliance-grade trail for system and operator actions.
- Fields:
  - `auditEventId` (UUID)
  - `entityType` (string)
  - `entityId` (string)
  - `action` (string)
  - `actorId` (string)
  - `actorType` (enum: SYSTEM, USER, SERVICE)
  - `traceId` (string)
  - `occurredAt` (timestamp)
  - `beforeRef` (string, nullable)
  - `afterRef` (string, nullable)
- Validation Rules:
  - Events are append-only.
  - `actorId`, `occurredAt`, and `traceId` are mandatory.

## Entity: RetentionPolicyRule
- Description: Data lifecycle policy definition for payloads, logs, and audit metadata.
- Fields:
  - `policyId` (string)
  - `artifactClass` (enum: RAW_PAYLOAD, INTERACTION_LOG, AUDIT_EVENT, DLQ_RECORD)
  - `retentionDays` (integer)
  - `dispositionAction` (enum: DELETE, ARCHIVE, ANONYMIZE)
  - `legalHoldSupported` (boolean)
  - `version` (string)
- Validation Rules:
  - `retentionDays` must be positive.
  - `dispositionAction` must be compatible with traceability requirements.

## State Transitions
- ReservationRequestContext:
  - RECEIVED -> VALIDATED -> QUEUED -> PROCESSING -> COMPLETED
  - PROCESSING -> FAILED -> DLQ (terminal failure path)
- AsynchronousWorkItem:
  - READY -> RUNNING -> SUCCEEDED
  - RUNNING -> RETRY_WAIT -> READY (transient failure path)
  - RUNNING -> TERMINAL_FAILED (permanent or max attempts reached)
- DeadLetterItem:
  - PENDING -> IN_REVIEW -> REPLAYED | CLOSED
