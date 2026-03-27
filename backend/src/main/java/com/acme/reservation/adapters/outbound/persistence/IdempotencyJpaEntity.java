package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the idempotency_records table.
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyJpaEntity {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "operation_type", nullable = false)
    private String operationType;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "result_status", nullable = false)
    private String resultStatus;

    @Column(name = "response_digest")
    private String responseDigest;

    @Column(name = "first_seen_at", nullable = false)
    private String firstSeenAt;

    @Column(name = "completed_at")
    private String completedAt;

    @Column(name = "expires_at", nullable = false)
    private String expiresAt;

    protected IdempotencyJpaEntity() {}

    public static IdempotencyJpaEntity from(IdempotencyRecord domain) {
        IdempotencyJpaEntity e = new IdempotencyJpaEntity();
        e.idempotencyKey = domain.getIdempotencyKey();
        e.operationType = domain.getOperationType().name();
        e.reservationId = domain.getReservationId();
        e.resultStatus = domain.getResultStatus().name();
        e.responseDigest = domain.getResponseDigest();
        e.firstSeenAt = domain.getFirstSeenAt().toString();
        e.completedAt = domain.getCompletedAt() != null ? domain.getCompletedAt().toString() : null;
        e.expiresAt = domain.getExpiresAt().toString();
        return e;
    }

    public IdempotencyRecord toDomain() {
        return new IdempotencyRecord(
                idempotencyKey,
                IdempotencyRecord.OperationType.valueOf(operationType),
                reservationId,
                IdempotencyRecord.ResultStatus.valueOf(resultStatus),
                responseDigest,
                Instant.parse(firstSeenAt),
                completedAt != null ? Instant.parse(completedAt) : null,
                Instant.parse(expiresAt));
    }
}
