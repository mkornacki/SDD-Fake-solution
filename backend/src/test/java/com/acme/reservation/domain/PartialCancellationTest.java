package com.acme.reservation.domain;

import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationStatus;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import com.acme.reservation.domain.reservation.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T035: Unit tests for partial cancellation aggregate invariants.
 * Only the targeted room transitions — remaining rooms stay ACTIVE.
 */
@DisplayName("T035: Partial cancellation aggregate invariants")
class PartialCancellationTest {

    @Test
    @DisplayName("cancelRoom: only targeted room transitions to CANCELLATION_PENDING")
    void cancelRoom_onlyTargetedRoom_transitions() {
        RoomReservationItem room1 = buildRoom("r1");
        RoomReservationItem room2 = buildRoom("r2");

        Reservation reservation = Reservation.create(
                "partner-1", "guest:Jane:Doe", "USD", null, List.of(room1, room2));

        String roomItemId = reservation.getRooms().get(0).getRoomItemId();

        reservation.cancelRoom(roomItemId, "guest request",
                new BigDecimal("20.00"), new BigDecimal("130.00"), Instant.now());

        RoomReservationItem targetRoom = reservation.getRooms().stream()
                .filter(r -> r.getRoomItemId().equals(roomItemId))
                .findFirst().orElseThrow();
        RoomReservationItem otherRoom = reservation.getRooms().stream()
                .filter(r -> !r.getRoomItemId().equals(roomItemId))
                .findFirst().orElseThrow();

        assertThat(targetRoom.getStatus()).isEqualTo(RoomStatus.CANCELLATION_PENDING);
        assertThat(otherRoom.getStatus()).isEqualTo(RoomStatus.ACTIVE);
    }

    @Test
    @DisplayName("cancelRoom: reservation status becomes PARTIALLY_CANCELLED when one of two rooms is cancelled")
    void cancelRoom_oneOfTwoRooms_reservationBecomesPartiallyCancelled() {
        RoomReservationItem room1 = buildRoom("r1");
        RoomReservationItem room2 = buildRoom("r2");

        Reservation reservation = Reservation.create(
                "partner-1", "guest:Jane:Doe", "USD", null, List.of(room1, room2));
        reservation.activate();

        String roomItemId = reservation.getRooms().get(0).getRoomItemId();
        reservation.cancelRoom(roomItemId, "reason", BigDecimal.TEN, BigDecimal.TEN, Instant.now());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PARTIALLY_CANCELLED);
    }

    @Test
    @DisplayName("cancelRoom: reservation becomes CANCELLED when all rooms are cancelled")
    void cancelRoom_allRooms_reservationBecomesFullyCancelled() {
        RoomReservationItem room1 = buildRoom("r1");

        Reservation reservation = Reservation.create(
                "partner-1", "guest:Jane:Doe", "USD", null, List.of(room1));
        reservation.activate();

        String roomItemId = reservation.getRooms().get(0).getRoomItemId();
        reservation.cancelRoom(roomItemId, "reason", BigDecimal.TEN, BigDecimal.TEN, Instant.now());
        // confirm the cancellation
        reservation.confirmRoomCancellation(roomItemId, Instant.now());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelRoom: totals are recalculated after cancellation")
    void cancelRoom_totalRecalculated_afterCancellation() {
        RoomReservationItem room1 = buildRoom("r1");
        RoomReservationItem room2 = buildRoom("r2");

        Reservation reservation = Reservation.create(
                "partner-1", "guest:Jane:Doe", "USD", null, List.of(room1, room2));

        BigDecimal originalTotal = reservation.getTotalPrice();

        String roomItemId = reservation.getRooms().get(0).getRoomItemId();
        reservation.cancelRoom(roomItemId, "reason",
                new BigDecimal("20.00"), new BigDecimal("80.00"), Instant.now());

        assertThat(reservation.getTotalCancellationFee()).isGreaterThan(BigDecimal.ZERO);
        assertThat(reservation.getTotalRefundAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    private RoomReservationItem buildRoom(String discriminator) {
        return RoomReservationItem.builder()
                .roomCode("RMX-" + discriminator)
                .reservationId("res-test")
                .checkInDate(LocalDate.now().plusDays(7))
                .checkOutDate(LocalDate.now().plusDays(10))
                .basePrice(new BigDecimal("100.00"))
                .build();
    }
}
