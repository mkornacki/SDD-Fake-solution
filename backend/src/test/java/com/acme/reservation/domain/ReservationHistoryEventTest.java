package com.acme.reservation.domain;

import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationHistoryEventTest {

    @Test
    void build_withAllRequiredFields_succeeds() {
        ReservationHistoryEvent event = ReservationHistoryEvent.builder()
                .reservationId("res-1")
                .eventType(ReservationHistoryEvent.EventType.CREATED)
                .actorId("user-1")
                .actorType(ReservationHistoryEvent.ActorType.USER)
                .traceId("trace-1")
                .occurredAt(Instant.now())
                .build();

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getReservationId()).isEqualTo("res-1");
        assertThat(event.getEventType()).isEqualTo(ReservationHistoryEvent.EventType.CREATED);
    }

    @Test
    void build_withoutReservationId_throwsException() {
        assertThatThrownBy(() ->
                ReservationHistoryEvent.builder()
                        .eventType(ReservationHistoryEvent.EventType.CREATED)
                        .actorId("user-1")
                        .actorType(ReservationHistoryEvent.ActorType.USER)
                        .traceId("trace-1")
                        .occurredAt(Instant.now())
                        .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reservationId");
    }

    @Test
    void historyEvent_hasNoSetterMethods() {
        // Verify immutability: the class should have no setter methods
        boolean hasSetters = java.util.Arrays.stream(ReservationHistoryEvent.class.getMethods())
                .anyMatch(m -> m.getName().startsWith("set"));
        assertThat(hasSetters)
                .as("ReservationHistoryEvent should have no setter methods (immutable)")
                .isFalse();
    }

    @Test
    void historyEvent_roomItemId_canBeNull_forReservationLevelEvents() {
        ReservationHistoryEvent event = ReservationHistoryEvent.builder()
                .reservationId("res-1")
                .eventType(ReservationHistoryEvent.EventType.RESERVATION_CANCELLED)
                .actorId("user-1")
                .actorType(ReservationHistoryEvent.ActorType.USER)
                .traceId("trace-1")
                .occurredAt(Instant.now())
                .build();

        assertThat(event.getRoomItemId()).isNull();
    }

    @Test
    void historyEvent_canHaveRoomItemId_forRoomLevelEvents() {
        ReservationHistoryEvent event = ReservationHistoryEvent.builder()
                .reservationId("res-1")
                .roomItemId("room-item-1")
                .eventType(ReservationHistoryEvent.EventType.ROOM_CANCELLED)
                .actorId("user-1")
                .actorType(ReservationHistoryEvent.ActorType.USER)
                .traceId("trace-1")
                .occurredAt(Instant.now())
                .build();

        assertThat(event.getRoomItemId()).isEqualTo("room-item-1");
    }
}
