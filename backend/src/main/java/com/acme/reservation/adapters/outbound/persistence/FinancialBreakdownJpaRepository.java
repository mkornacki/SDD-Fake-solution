package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.application.ports.outbound.FinancialBreakdownRepository;
import com.acme.reservation.domain.financial.FinancialBreakdown;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence adapter for FinancialBreakdownRepository.
 */
@Repository
public class FinancialBreakdownJpaRepository implements FinancialBreakdownRepository {

    private final FinancialBreakdownSpringDataRepository breakdownRepo;
    private final FinancialLineItemSpringDataRepository lineItemRepo;

    public FinancialBreakdownJpaRepository(
            FinancialBreakdownSpringDataRepository breakdownRepo,
            FinancialLineItemSpringDataRepository lineItemRepo) {
        this.breakdownRepo = breakdownRepo;
        this.lineItemRepo = lineItemRepo;
    }

    @Override
    @Transactional
    public FinancialBreakdown save(FinancialBreakdown breakdown) {
        FinancialBreakdownJpaEntity entity = FinancialBreakdownJpaEntity.from(breakdown);
        breakdownRepo.save(entity);

        // delete old line items and re-save (full replace)
        List<FinancialLineItemJpaEntity> existing = lineItemRepo.findByBreakdownId(breakdown.getBreakdownId());
        lineItemRepo.deleteAll(existing);

        List<FinancialLineItemJpaEntity> lineItemEntities = breakdown.getLineItems().stream()
                .map(item -> FinancialLineItemJpaEntity.from(item, breakdown.getBreakdownId()))
                .collect(Collectors.toList());
        lineItemRepo.saveAll(lineItemEntities);

        return breakdown;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FinancialBreakdown> findByReservationId(String reservationId) {
        return breakdownRepo.findByReservationId(reservationId)
                .map(entity -> {
                    List<FinancialLineItemJpaEntity> lineItems =
                            lineItemRepo.findByBreakdownId(entity.getBreakdownId());
                    return entity.toDomain(lineItems);
                });
    }
}
