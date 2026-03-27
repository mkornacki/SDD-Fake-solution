package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the reservation_history_events table.
 */
@Entity
@Table(name = "reservation_history_events")
public class HistoryEventJpaEntity {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "room_item_id")
    private String roomItemId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "before_state_ref")
    private String beforeStateRef;

    @Column(name = "after_state_ref")
    private String afterStateRef;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "occurred_at", nullable = false)
    private String occurredAt;

    protected HistoryEventJpaEntity() {
    }

    public static HistoryEventJpaEntity from(ReservationHistoryEvent domain) {
        HistoryEventJpaEntity e = new HistoryEventJpaEntity();
        e.eventId = domain.getEventId();
        e.reservationId = domain.getReservationId();
        e.roomItemId = domain.getRoomItemId();
        e.eventType = domain.getEventType().name();
        e.actorId = domain.getActorId();
        e.actorType = domain.getActorType().name();
        e.reason = domain.getReason();
        e.beforeStateRef = domain.getBeforeStateRef();
        e.afterStateRef = domain.getAfterStateRef();
        e.traceId = domain.getTraceId();
        e.occurredAt = domain.getOccurredAt().toString();
        return e;
    }

    public ReservationHistoryEvent toDomain() {
        return ReservationHistoryEvent.builder()
                .eventId(eventId)
                .reservationId(reservationId)
                .roomItemId(roomItemId)
                .eventType(ReservationHistoryEvent.EventType.valueOf(eventType))
                .actorId(actorId)
                .actorType(ReservationHistoryEvent.ActorType.valueOf(actorType))
                .reason(reason)
                .beforeStateRef(beforeStateRef)
                .afterStateRef(afterStateRef)
                .traceId(traceId)
                .occurredAt(Instant.parse(occurredAt))
                .build();
    }
}
