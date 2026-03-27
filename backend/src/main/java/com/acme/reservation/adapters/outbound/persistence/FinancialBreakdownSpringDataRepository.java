package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for financial_breakdowns table.
 */
public interface FinancialBreakdownSpringDataRepository
        extends JpaRepository<FinancialBreakdownJpaEntity, String> {

    Optional<FinancialBreakdownJpaEntity> findByReservationId(String reservationId);
}
