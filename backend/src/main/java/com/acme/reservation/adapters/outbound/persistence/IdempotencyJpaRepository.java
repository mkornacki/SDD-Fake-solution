package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter for idempotency record persistence.
 * Unique constraint on idempotency_key enforced at DB level.
 */
@Repository
public class IdempotencyJpaRepository implements IdempotencyRepository {

    private final IdempotencySpringDataRepository springDataRepo;

    public IdempotencyJpaRepository(IdempotencySpringDataRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        IdempotencyJpaEntity entity = IdempotencyJpaEntity.from(record);
        IdempotencyJpaEntity saved = springDataRepo.saveAndFlush(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        return springDataRepo.findById(idempotencyKey)
                .map(IdempotencyJpaEntity::toDomain);
    }

    @Override
    public long deleteExpiredBefore(java.time.Instant cutoff) {
        return (long) springDataRepo.deleteByExpiresAtBefore(cutoff.toString());
    }

}
