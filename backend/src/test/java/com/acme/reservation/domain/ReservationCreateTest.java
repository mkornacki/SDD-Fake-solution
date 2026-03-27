package com.acme.reservation.domain;

import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationStatus;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import com.acme.reservation.domain.reservation.RoomStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationCreateTest {

    @Test
    void createReservation_withValidRooms_succeeds() {
        RoomReservationItem room = sampleRoom("ROOM-101");
        Reservation r = Reservation.create("partner-1", "guest-token-1", "USD", null, List.of(room));

        assertThat(r.getReservationId()).isNotNull();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(r.getPartnerId()).isEqualTo("partner-1");
        assertThat(r.getRooms()).hasSize(1);
        assertThat(r.getTotalPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void createReservation_withMultipleRooms_sumsTotalPrice() {
        RoomReservationItem r1 = sampleRoom("ROOM-101");
        RoomReservationItem r2 = sampleRoomWithPrice("ROOM-102", new BigDecimal("250.00"));
        Reservation r = Reservation.create("partner-1", "guest-token-1", "USD", null, List.of(r1, r2));

        assertThat(r.getRooms()).hasSize(2);
        assertThat(r.getTotalPrice()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(r.getRoomCount()).isEqualTo(2);
    }

    @Test
    void createReservation_withNoRooms_throwsException() {
        assertThatThrownBy(() ->
                Reservation.create("partner-1", "guest-token-1", "USD", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one room");
    }

    @Test
    void createReservation_withNullRooms_throwsException() {
        assertThatThrownBy(() ->
                Reservation.create("partner-1", "guest-token-1", "USD", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createReservation_missingPartnerId_throwsException() {
        RoomReservationItem room = sampleRoom("ROOM-101");
        assertThatThrownBy(() ->
                Reservation.create(null, "guest-token-1", "USD", null, List.of(room)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void activate_transitionsFromPendingToActive() {
        RoomReservationItem room = sampleRoom("ROOM-101");
        Reservation r = Reservation.create("partner-1", "guest-token-1", "USD", null, List.of(room));
        r.activate();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void roomStatus_initiallyActive() {
        RoomReservationItem room = sampleRoom("ROOM-101");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(room.isActive()).isTrue();
        assertThat(room.isCancelled()).isFalse();
    }

    private RoomReservationItem sampleRoom(String code) {
        return sampleRoomWithPrice(code, new BigDecimal("150.00"));
    }

    private RoomReservationItem sampleRoomWithPrice(String code, BigDecimal price) {
        return RoomReservationItem.builder()
                .roomCode(code)
                .reservationId("res-test-1")
                .checkInDate(LocalDate.of(2026, 6, 1))
                .checkOutDate(LocalDate.of(2026, 6, 5))
                .basePrice(price)
                .build();
    }
}
