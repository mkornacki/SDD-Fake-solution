package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for financial_line_items table.
 */
public interface FinancialLineItemSpringDataRepository
        extends JpaRepository<FinancialLineItemJpaEntity, String> {

    List<FinancialLineItemJpaEntity> findByBreakdownId(String breakdownId);
}
