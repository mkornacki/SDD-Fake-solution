package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.reservation.ReservationRequestContext;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for ReservationRequestContext domain aggregate.
 * Maps to reservation_request_contexts table.
 */
@Entity
@Table(name = "reservation_request_contexts")
public class RequestContextJpaEntity {

  @Id
  @Column(name = "context_id")
  private String contextId;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "correlation_id", nullable = false)
  private String correlationId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "request_payload_ref")
  private String requestPayloadRef;

  @Column(name = "response_payload_ref")
  private String responsePayloadRef;

  @Column(name = "error_context")
  private String errorContext;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // Constructors
  protected RequestContextJpaEntity() {}

  private RequestContextJpaEntity(String contextId, String idempotencyKey, String correlationId,
      String status, String requestPayloadRef, String responsePayloadRef, String errorContext,
      Instant createdAt, Instant updatedAt) {
    this.contextId = contextId;
    this.idempotencyKey = idempotencyKey;
    this.correlationId = correlationId;
    this.status = status;
    this.requestPayloadRef = requestPayloadRef;
    this.responsePayloadRef = responsePayloadRef;
    this.errorContext = errorContext;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /**
   * Convert domain entity to JPA entity.
   */
  public static RequestContextJpaEntity from(ReservationRequestContext domain) {
    return new RequestContextJpaEntity(
        domain.getContextId(),
        domain.getIdempotencyKey(),
        domain.getCorrelationId(),
        domain.getStatus().name(),
        domain.getRequestPayloadRef(),
        domain.getResponsePayloadRef(),
        domain.getErrorContext(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }

  /**
   * Convert JPA entity to domain entity.
   */
  public ReservationRequestContext toDomain() {
    ReservationRequestContext context = ReservationRequestContext.create(
        this.idempotencyKey, this.correlationId);
    context.setContextId(this.contextId);
    context.setStatus(ReservationRequestContext.Status.valueOf(this.status));
    context.setRequestPayloadRef(this.requestPayloadRef);
    context.setResponsePayloadRef(this.responsePayloadRef);
    context.setErrorContext(this.errorContext);
    context.setCreatedAt(this.createdAt);
    context.setUpdatedAt(this.updatedAt);
    return context;
  }

  // Getters and setters
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
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
