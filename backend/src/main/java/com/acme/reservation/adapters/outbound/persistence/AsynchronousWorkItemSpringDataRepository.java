package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data access for asynchronous work items.
 */
interface AsynchronousWorkItemSpringDataRepository extends JpaRepository<AsynchronousWorkItemJpaEntity, String> {

    @Query("SELECT w FROM AsynchronousWorkItemJpaEntity w WHERE w.status = 'READY'")
    List<AsynchronousWorkItemJpaEntity> findReadyItems();

    @Query("SELECT w FROM AsynchronousWorkItemJpaEntity w WHERE w.status = 'RETRY_WAIT' AND w.nextAttemptAt <= ?1")
    List<AsynchronousWorkItemJpaEntity> findRetryDueItems(String nowIso);
}
