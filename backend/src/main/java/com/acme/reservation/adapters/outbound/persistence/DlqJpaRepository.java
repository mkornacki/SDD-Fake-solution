package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.application.ports.outbound.DlqRepository;
import com.acme.reservation.domain.audit.DlqItem;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter implementing DlqRepository outbound port.
 */
@Repository
public class DlqJpaRepository implements DlqRepository {

    private final DlqSpringDataRepository springDataRepo;

    public DlqJpaRepository(DlqSpringDataRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public DlqItem save(DlqItem dlqItem) {
        DlqJpaEntity entity = DlqJpaEntity.from(dlqItem);
        return springDataRepo.save(entity).toDomain();
    }

    @Override
    public Optional<DlqItem> findById(String dlqId) {
        return springDataRepo.findById(dlqId).map(DlqJpaEntity::toDomain);
    }
}
