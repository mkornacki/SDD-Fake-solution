package com.acme.reservation.domain.reservation;

import java.time.Instant;
import java.util.UUID;

/**
 * ReservationRequestContext tracks the lifecycle of a reservation request from acceptance through
 * completion, including intermediate states during asynchronous processing.
 *
 * This is a domain aggregate that models the business state machine for resilient reservation
 * operations. Status transitions are: RECEIVED → VALIDATED → QUEUED → PROCESSING → COMPLETED
 * (or PROCESSING → FAILED or PROCESSING → DLQ for error scenarios).
 */
public class ReservationRequestContext {

  public enum Status {
    RECEIVED,
    VALIDATED,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    DLQ
  }

  private String contextId;
  private String idempotencyKey;
  private String correlationId;
  private Status status;
  private String requestPayloadRef;
  private String responsePayloadRef;
  private String errorContext;
  private Instant createdAt;
  private Instant updatedAt;

  protected ReservationRequestContext() {
    // For ORM reconstruction
  }

  /**
   * Factory method to create a new ReservationRequestContext for an initial create request.
   *
   * @param idempotencyKey unique idempotency key from the client
   * @param correlationId  unique request correlation ID for tracing
   * @return new context in RECEIVED status
   */
  public static ReservationRequestContext create(String idempotencyKey, String correlationId) {
    var context = new ReservationRequestContext();
    context.contextId = UUID.randomUUID().toString();
    context.idempotencyKey = idempotencyKey;
    context.correlationId = correlationId;
    context.status = Status.RECEIVED;
    context.createdAt = Instant.now();
    context.updatedAt = Instant.now();
    return context;
  }

  /**
   * Transition to VALIDATED state after request validation passes.
   */
  public void markValidated() {
    validateTransition(Status.VALIDATED);
    this.status = Status.VALIDATED;
    this.updatedAt = Instant.now();
  }

  /**
   * Transition to QUEUED state after request is enqueued for async processing.
   */
  public void markQueued() {
    validateTransition(Status.QUEUED);
    this.status = Status.QUEUED;
    this.updatedAt = Instant.now();
  }

  /**
   * Transition to PROCESSING state when work item begins executing.
   */
  public void markProcessing() {
    validateTransition(Status.PROCESSING);
    this.status = Status.PROCESSING;
    this.updatedAt = Instant.now();
  }

  /**
   * Transition to COMPLETED state after successful processing.
   */
  public void markCompleted() {
    validateTransition(Status.COMPLETED);
    this.status = Status.COMPLETED;
    this.updatedAt = Instant.now();
  }

  /**
   * Transition to FAILED state after terminal failure.
   *
   * @param errorMessage optional error context for diagnostics
   */
  public void markFailed(String errorMessage) {
    validateTransition(Status.FAILED);
    this.status = Status.FAILED;
    this.errorContext = errorMessage;
    this.updatedAt = Instant.now();
  }

  /**
   * Transition to DLQ state when routed to dead-letter queue.
   *
   * @param errorMessage optional error context for diagnostics
   */
  public void markDlq(String errorMessage) {
    validateTransition(Status.DLQ);
    this.status = Status.DLQ;
    this.errorContext = errorMessage;
    this.updatedAt = Instant.now();
  }

  /**
   * Validate the state machine transition.
   *
   * @param targetStatus target status for this transition
   * @throws IllegalStateException if transition is invalid
   */
  private void validateTransition(Status targetStatus) {
    // Terminal states cannot transition further
    if (this.status == Status.COMPLETED || this.status == Status.FAILED || this.status == Status.DLQ) {
      throw new IllegalStateException("Cannot transition from terminal state: " + this.status);
    }

    // Define allowed transitions
    if (this.status == Status.RECEIVED && targetStatus != Status.VALIDATED) {
      throw new IllegalStateException("From RECEIVED, only VALIDATED transition is allowed");
    }

    if (this.status == Status.VALIDATED && targetStatus != Status.QUEUED) {
      throw new IllegalStateException("From VALIDATED, only QUEUED transition is allowed");
    }

    if (this.status == Status.QUEUED && targetStatus != Status.PROCESSING) {
      throw new IllegalStateException("From QUEUED, only PROCESSING transition is allowed");
    }

    if (this.status == Status.PROCESSING) {
      if (targetStatus != Status.COMPLETED && targetStatus != Status.FAILED && targetStatus != Status.DLQ) {
        throw new IllegalStateException("From PROCESSING, only COMPLETED, FAILED, or DLQ transitions are allowed");
      }
    }
  }

  /**
   * Check if this context is in a terminal state (no further transitions possible).
   *
   * @return true if status is COMPLETED, FAILED, or DLQ
   */
  public boolean isTerminal() {
    return status == Status.COMPLETED || status == Status.FAILED || status == Status.DLQ;
  }

  // --- Getters and Setters ---

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getRequestPayloadRef() {
    return requestPayloadRef;
  }

  public void setRequestPayloadRef(String requestPayloadRef) {
    this.requestPayloadRef = requestPayloadRef;
  }

  public String getResponsePayloadRef() {
    return responsePayloadRef;
  }

  public void setResponsePayloadRef(String responsePayloadRef) {
    this.responsePayloadRef = responsePayloadRef;
  }

  public String getErrorContext() {
    return errorContext;
  }

  public void setErrorContext(String errorContext) {
    this.errorContext = errorContext;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
