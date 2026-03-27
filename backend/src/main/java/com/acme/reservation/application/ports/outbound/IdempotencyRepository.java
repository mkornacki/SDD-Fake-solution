package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.idempotency.IdempotencyRecord;

import java.util.Optional;

/**
 * Outbound port for idempotency record persistence.
 */
public interface IdempotencyRepository {
    IdempotencyRecord save(IdempotencyRecord record);

    Optional<IdempotencyRecord> findByKey(String idempotencyKey);

    /**
     * Delete idempotency records that expire strictly before the given cutoff.
     * Returns number of records deleted.
     */
    long deleteExpiredBefore(java.time.Instant cutoff);
}
