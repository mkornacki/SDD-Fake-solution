package com.acme.reservation.domain.idempotency;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Objects;

/**
 * IdempotencyRecord tracks a unique idempotency key and its associated business fingerprint,
 * ensuring that duplicate create requests with the same key produce only one canonical outcome.
 *
 * This entity enforces the idempotency guarantee: repeated requests with the same key
 * will return the same result, preventing accidental duplicates even under concurrent retries.
 *
 * Idempotency records expire and are subject to cleanup after a configurable retention period
 * (default 30 days).
 */
public class IdempotencyRecord {

  public enum OperationType {
    CREATE, CANCEL_RESERVATION, CANCEL_ROOM
  }

  public enum ResultStatus {
    IN_PROGRESS, COMPLETED, FAILED
  }

  private final String id;
  private final String idempotencyKey;
  private String fingerprint;
  private final Instant createdAt;
  private final Instant expiresAt;
  private final OperationType operationType;
  private String reservationId;
  private ResultStatus resultStatus;
  private String responseDigest;
  private Instant completedAt;

  /**
   * Constructor for creating a new idempotency record with operation type and default 30-day retention.
   *
   * @param idempotencyKey unique key provided by the client
   * @param operationType  the type of operation (CREATE, CANCEL_RESERVATION, etc.)
   * @param firstSeenAt    when this record was first created
   * @param expiresAt      when this record expires
   */
  public IdempotencyRecord(String idempotencyKey, OperationType operationType,
      Instant firstSeenAt, Instant expiresAt) {
    this.id = UUID.randomUUID().toString();
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey required");
    this.operationType = Objects.requireNonNull(operationType, "operationType required");
    this.createdAt = Objects.requireNonNull(firstSeenAt, "firstSeenAt required");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt required");
    this.resultStatus = ResultStatus.IN_PROGRESS;
  }

  /**
   * Constructor for creating a new idempotency record with fingerprint and default 30-day retention.
   *
   * @param idempotencyKey unique key provided by the client
   * @param fingerprint    deterministic fingerprint of the business context
   * @param retentionDays  number of days to retain this record
   */
  public IdempotencyRecord(String idempotencyKey, String fingerprint, int retentionDays) {
    this.id = UUID.randomUUID().toString();
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey required");
    this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint required");
    this.createdAt = Instant.now();
    this.expiresAt = this.createdAt.plus(retentionDays, ChronoUnit.DAYS);
    this.operationType = OperationType.CREATE;
    this.resultStatus = ResultStatus.IN_PROGRESS;
  }

  /**
   * Constructor for creating a new idempotency record with fingerprint and default 30-day retention.
   *
   * @param idempotencyKey unique key provided by the client
   * @param fingerprint    deterministic fingerprint of the business context
   */
  public IdempotencyRecord(String idempotencyKey, String fingerprint) {
    this(idempotencyKey, fingerprint, 30);
  }

  /**
   * Reconstruction constructor (from persistence with all fields).
   */
  public IdempotencyRecord(String id, String idempotencyKey, String fingerprint,
      Instant createdAt, Instant expiresAt) {
    this.id = Objects.requireNonNull(id, "id required");
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey required");
    this.fingerprint = fingerprint;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt required");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt required");
    this.operationType = OperationType.CREATE;
    this.resultStatus = ResultStatus.IN_PROGRESS;
  }

  // --- Reconstruction constructor (from persistence) ---

  public IdempotencyRecord(String idempotencyKey, OperationType operationType,
      String reservationId, ResultStatus resultStatus, String responseDigest,
      Instant firstSeenAt, Instant completedAt, Instant expiresAt) {
    this.id = UUID.randomUUID().toString();
    this.idempotencyKey = idempotencyKey;
    this.operationType = operationType;
    this.reservationId = reservationId;
    this.resultStatus = resultStatus;
    this.responseDigest = responseDigest;
    this.createdAt = firstSeenAt;
    this.completedAt = completedAt;
    this.expiresAt = expiresAt;
  }

  /**
   * Complete the idempotency record after successful processing.
   */
  public void complete(String reservationId, String responseDigest, Instant now) {
    Objects.requireNonNull(reservationId, "reservationId required to complete");
    this.reservationId = reservationId;
    this.responseDigest = responseDigest;
    this.resultStatus = ResultStatus.COMPLETED;
    this.completedAt = now;
  }

  /**
   * Mark the record as failed.
   */
  public void fail(Instant now) {
    this.resultStatus = ResultStatus.FAILED;
    this.completedAt = now;
  }

  /**
   * Check if this idempotency record has expired.
   *
   * @return true if current time is past expiresAt
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /**
   * Validate that an incoming request's fingerprint matches this record's fingerprint,
   * confirming the same business context.
   *
   * @param incomingFingerprint fingerprint from the incoming request
   * @return true if fingerprints match (same idempotency context)
   */
  public boolean matchesFingerprint(String incomingFingerprint) {
    return fingerprint != null && fingerprint.equals(incomingFingerprint);
  }

  public boolean isInProgress() {
    return resultStatus == ResultStatus.IN_PROGRESS;
  }

  public boolean isCompleted() {
    return resultStatus == ResultStatus.COMPLETED;
  }

  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now);
  }

  // --- Getters ---

  public String getId() {
    return id;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public String getReservationId() {
    return reservationId;
  }

  public ResultStatus getResultStatus() {
    return resultStatus;
  }

  public String getResponseDigest() {
    return responseDigest;
  }

  public Instant getFirstSeenAt() {
    return createdAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
