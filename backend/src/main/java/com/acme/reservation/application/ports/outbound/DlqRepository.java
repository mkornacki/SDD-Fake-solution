package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.audit.DlqItem;

import java.util.Optional;

/**
 * Outbound port for dead-letter queue persistence.
 */
public interface DlqRepository {
    DlqItem save(DlqItem dlqItem);

    Optional<DlqItem> findById(String dlqId);
}
