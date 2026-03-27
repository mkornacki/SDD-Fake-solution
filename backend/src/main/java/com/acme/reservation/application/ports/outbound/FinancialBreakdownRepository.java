package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.financial.FinancialBreakdown;

import java.util.Optional;

/**
 * Outbound port for financial breakdown persistence.
 */
public interface FinancialBreakdownRepository {
    FinancialBreakdown save(FinancialBreakdown breakdown);

    Optional<FinancialBreakdown> findByReservationId(String reservationId);
}
