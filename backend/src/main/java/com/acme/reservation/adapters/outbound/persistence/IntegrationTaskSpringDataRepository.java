package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data JPA repository for IntegrationTaskJpaEntity.
 */
interface IntegrationTaskSpringDataRepository extends JpaRepository<IntegrationTaskJpaEntity, String> {
    @Query("SELECT t FROM IntegrationTaskJpaEntity t WHERE t.state IN ('READY', 'RETRY_WAIT')")
    List<IntegrationTaskJpaEntity> findPendingTasks();
}
