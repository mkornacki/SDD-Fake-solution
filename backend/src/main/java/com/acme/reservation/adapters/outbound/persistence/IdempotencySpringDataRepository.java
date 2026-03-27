package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for IdempotencyJpaEntity.
 */
interface IdempotencySpringDataRepository extends JpaRepository<IdempotencyJpaEntity, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM IdempotencyJpaEntity e WHERE e.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") String cutoff);
}
