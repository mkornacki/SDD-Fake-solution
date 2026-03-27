package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.reservation.ReservationHistoryEvent;

import java.util.List;

/**
 * Outbound port for audit history event persistence.
 */
public interface AuditEventRepository {
    ReservationHistoryEvent save(ReservationHistoryEvent event);

    List<ReservationHistoryEvent> findByReservationIdOrderedByOccurredAt(String reservationId);
}
