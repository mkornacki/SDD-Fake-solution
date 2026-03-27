# Data Model: Hotel Reservation Partner API

## Entity: Reservation (Aggregate Root)
- Description: Top-level booking aggregate representing a complete multi-room reservation with lifecycle state, financial totals, partner context, and customer references.
- Fields:
  - `reservationId` (UUID, required, immutable, globally unique)
  - `partnerId` (string, required)
  - `externalReference` (string, optional, partner-provided reference)
  - `status` (enum: PENDING, ACTIVE, PARTIALLY_CANCELLED, CANCELLED, FAILED)
  - `currencyCode` (string, required, ISO 4217)
  - `totalPrice` (BigDecimal, required, derived)
  - `totalRefundAmount` (BigDecimal, required, default 0)
  - `totalCancellationFee` (BigDecimal, required, default 0)
  - `roomCount` (integer, required, derived from active room items)
  - `version` (integer, optimistic lock version, required)
  - `createdAt` (timestamp, required, immutable)
  - `updatedAt` (timestamp, required)
  - `guestId` (string, required, tokenized reference to PII store)
- Validation Rules:
  - `totalPrice` MUST equal sum of active `RoomReservationItem.basePrice` minus cancellation fees plus any adjustments.
  - `status` MUST follow defined state transition rules.
  - `version` used for optimistic concurrency control.
- Relationships:
  - One-to-many with `RoomReservationItem`.
  - One-to-many with `ReservationHistoryEvent`.
  - One-to-one with `IdempotencyRecord` (keyed on create operation).

## Entity: RoomReservationItem
- Description: Independently manageable room booking within a reservation aggregate, with occupancy dates, pricing, and cancellation state.
- Fields:
  - `roomItemId` (UUID, required, immutable)
  - `reservationId` (UUID, required)
  - `roomCode` (string, required)
  - `checkInDate` (date, required)
  - `checkOutDate` (date, required)
  - `status` (enum: ACTIVE, CANCELLATION_PENDING, CANCELLED)
  - `basePrice` (BigDecimal, required)
  - `cancellationFee` (BigDecimal, nullable)
  - `refundAmount` (BigDecimal, nullable)
  - `cancellationReason` (string, optional)
  - `cancelledAt` (timestamp, nullable)
  - `processingStatus` (enum: NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED)
- Validation Rules:
  - `checkOutDate` MUST be after `checkInDate`.
  - Transition from ACTIVE to CANCELLED MUST be idempotent.
  - `basePrice` MUST be positive.
- Relationships:
  - Many-to-one to `Reservation`.
  - One-to-many with `IntegrationTask`.

## Entity: ReservationHistoryEvent
- Description: Immutable chronological state transition record for reservation-level and room-level operations. Append-only; never mutated.
- Fields:
  - `eventId` (UUID, required, immutable)
  - `reservationId` (UUID, required)
  - `roomItemId` (UUID, optional; null for reservation-level events)
  - `eventType` (enum: CREATED, ROOM_CANCELLED, RESERVATION_CANCELLED, STATUS_CHANGED, FINANCIAL_RECALCULATED, ASYNC_FAILED, REPLAYED)
  - `actorId` (string, required, from authentication context)
  - `actorType` (enum: USER, PARTNER, SERVICE, SYSTEM)
  - `reason` (string, optional)
  - `beforeStateRef` (string, nullable, snapshot reference)
  - `afterStateRef` (string, nullable, snapshot reference)
  - `traceId` (string, required)
  - `occurredAt` (timestamp, required)
- Validation Rules:
  - Events are append-only; MUST NOT be updated or deleted.
  - `actorId`, `eventType`, `traceId`, and `occurredAt` are mandatory.

## Entity: IdempotencyRecord
- Description: Deduplication guard ensuring repeated create or cancellation requests map to one canonical outcome.
- Fields:
  - `idempotencyKey` (string, primary key, client-provided)
  - `operationType` (enum: CREATE, CANCEL_RESERVATION, CANCEL_ROOM)
  - `reservationId` (UUID, nullable, populated on completion)
  - `resultStatus` (enum: IN_PROGRESS, COMPLETED, FAILED)
  - `responseDigest` (string, nullable, hash of canonical response)
  - `firstSeenAt` (timestamp, required)
  - `completedAt` (timestamp, nullable)
  - `expiresAt` (timestamp, required, policy-driven, default 30 days)
- Validation Rules:
  - Unique constraint on `idempotencyKey`.
  - Transition to COMPLETED requires non-null `reservationId`.
  - Expired records may be purged without affecting active reservations.

## Entity: FinancialBreakdown
- Description: Itemized financial summary for a reservation, capturing charges, taxes, fees, penalties, and refund adjustments for reconciliation.
- Fields:
  - `breakdownId` (UUID, required)
  - `reservationId` (UUID, required)
  - `lineItems` (list of FinancialLineItem)
  - `subtotal` (BigDecimal, required)
  - `totalTax` (BigDecimal, required)
  - `totalFees` (BigDecimal, required)
  - `totalPenalties` (BigDecimal, required)
  - `totalRefunds` (BigDecimal, required)
  - `netTotal` (BigDecimal, required, derived)
  - `calculatedAt` (timestamp, required)
- Sub-entity: `FinancialLineItem`
  - `lineType` (enum: BASE_PRICE, TAX, FEE, CANCELLATION_PENALTY, REFUND)
  - `description` (string, required)
  - `amount` (BigDecimal, required)
  - `roomItemId` (UUID, optional)
- Validation Rules:
  - `netTotal` MUST equal `subtotal + totalTax + totalFees + totalPenalties - totalRefunds`.
  - All amounts MUST use the same currency as the parent `Reservation.currencyCode`.

## Entity: IntegrationTask
- Description: Asynchronous interaction unit for partner or payment system communication, tracking execution state and retry history.
- Fields:
  - `taskId` (UUID, required, immutable)
  - `reservationId` (UUID, required)
  - `roomItemId` (UUID, optional)
  - `taskType` (enum: PARTNER_CREATE, PARTNER_CANCEL, REFUND_INITIATE)
  - `state` (enum: READY, RUNNING, RETRY_WAIT, SUCCEEDED, TERMINAL_FAILED)
  - `attemptCount` (integer, default 0)
  - `maxAttempts` (integer, required)
  - `nextAttemptAt` (timestamp, nullable)
  - `lastFailureReason` (string, nullable)
  - `failureClass` (enum: TRANSIENT, PERMANENT, NONE)
  - `createdAt` (timestamp, required)
  - `updatedAt` (timestamp, required)
- Validation Rules:
  - `attemptCount` MUST NOT exceed `maxAttempts` before transitioning to TERMINAL_FAILED.
  - `nextAttemptAt` MUST be set when `state=RETRY_WAIT`.
- Relationships:
  - Many-to-one to `RoomReservationItem`.
  - One-to-one with `DLQItem` (on terminal failure).

## Entity: DLQItem
- Description: Terminally failed integration task preserved for operator triage and replay.
- Fields:
  - `dlqId` (UUID, required, immutable)
  - `taskId` (UUID, required)
  - `reservationId` (UUID, required)
  - `failureReason` (string, required)
  - `attemptHistoryRef` (string, required, link to attempt log)
  - `maskedPayloadRef` (string, required, link to masked payload snapshot)
  - `replayStatus` (enum: PENDING, IN_REVIEW, REPLAYED, CLOSED)
  - `replayCount` (integer, default 0)
  - `createdAt` (timestamp, required)
- Validation Rules:
  - Replay preserves `IdempotencyRecord` semantics; duplicate replay MUST NOT create new reservations.

## Entity: PIIAccessContext
- Description: Runtime representation of caller's PII authorization scope derived from JWT, used to govern masking/unmasking in API responses. Not persisted.
- Fields:
  - `callerId` (string, required)
  - `allowedScopes` (string list, from JWT claims)
  - `hasPIIAccess` (boolean, derived from `allowedScopes`)
- Validation Rules:
  - PII fields in responses MUST be masked when `hasPIIAccess=false`.
  - Masking is applied at the adapter layer, not the domain layer.

## State Transitions
- Reservation:
  - PENDING → ACTIVE (first room confirmed)
  - ACTIVE → PARTIALLY_CANCELLED (one or more rooms cancelled, at least one remains active)
  - ACTIVE / PARTIALLY_CANCELLED → CANCELLED (all rooms cancelled or full cancellation requested)
  - ACTIVE → FAILED (terminal async failure with no recovery path)
- RoomReservationItem:
  - ACTIVE → CANCELLATION_PENDING (cancellation accepted, async processing started)
  - CANCELLATION_PENDING → CANCELLED (partner confirmation received)
  - CANCELLATION_PENDING → ACTIVE (rollback on unrecoverable failure, if business policy permits)
- IntegrationTask:
  - READY → RUNNING → SUCCEEDED
  - RUNNING → RETRY_WAIT → READY (transient failure)
  - RUNNING → TERMINAL_FAILED (permanent failure or max attempts)
- DLQItem:
  - PENDING → IN_REVIEW → REPLAYED | CLOSED
