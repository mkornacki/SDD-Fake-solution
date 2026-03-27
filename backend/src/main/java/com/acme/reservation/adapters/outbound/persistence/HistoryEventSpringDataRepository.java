package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for HistoryEventJpaEntity.
 */
interface HistoryEventSpringDataRepository extends JpaRepository<HistoryEventJpaEntity, String> {
    @Query("SELECT e FROM HistoryEventJpaEntity e WHERE e.reservationId = :resId ORDER BY e.occurredAt ASC")
    List<HistoryEventJpaEntity> findByReservationIdOrderByOccurredAtAsc(
            @Param("resId") String reservationId);
}
