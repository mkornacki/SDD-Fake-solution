package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import com.acme.reservation.application.ports.outbound.AuditEventRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA adapter for reservation history event persistence (T034, US2).
 * History events are append-only; no update operations.
 */
@Repository
public class HistoryEventJpaRepository implements AuditEventRepository {

    private final HistoryEventSpringDataRepository springDataRepo;

    public HistoryEventJpaRepository(HistoryEventSpringDataRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public ReservationHistoryEvent save(ReservationHistoryEvent event) {
        HistoryEventJpaEntity entity = HistoryEventJpaEntity.from(event);
        return springDataRepo.save(entity).toDomain();
    }

    @Override
    public List<ReservationHistoryEvent> findByReservationIdOrderedByOccurredAt(
            String reservationId) {
        return springDataRepo.findByReservationIdOrderByOccurredAtAsc(reservationId)
                .stream()
                .map(HistoryEventJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

}
