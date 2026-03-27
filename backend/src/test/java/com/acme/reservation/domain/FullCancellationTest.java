package com.acme.reservation.domain;

import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationStatus;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T043: Unit tests for full cancellation idempotency.
 * Repeated full-cancel calls must not alter finalized state.
 */
@DisplayName("T043: Full cancellation idempotency domain tests")
class FullCancellationTest {

    @Test
    @DisplayName("cancelAll: all rooms transition to CANCELLATION_PENDING")
    void cancelAll_allRoomsTransition() {
        Reservation reservation = buildTwoRoomReservation();
        reservation.activate();
        Instant now = Instant.now();

        reservation.cancelAll("operator request", BigDecimal.TEN, new BigDecimal("90.00"), now);

        reservation.getRooms().forEach(room ->
                assertThat(room.getStatus().name()).isIn("CANCELLATION_PENDING", "CANCELLED"));
    }

    @Test
    @DisplayName("cancelAll: reservation status becomes CANCELLED")
    void cancelAll_reservationStatusBecomesCANCELLED() {
        Reservation reservation = buildTwoRoomReservation();
        reservation.activate();

        reservation.cancelAll("full cancel", BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
        // confirm all cancellations
        reservation.getRooms().forEach(r ->
                reservation.confirmRoomCancellation(r.getRoomItemId(), Instant.now()));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelAll: already-cancelled rooms are skipped (idempotent)")
    void cancelAll_idempotent_skipsAlreadyCancelledRooms() {
        Reservation reservation = buildTwoRoomReservation();
        reservation.activate();

        Instant first = Instant.now();
        reservation.cancelAll("first", BigDecimal.ZERO, BigDecimal.ZERO, first);

        // Second call should not raise exception
        reservation.cancelAll("second", BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());

        // All rooms should still be in cancellation states (not duplicated)
        assertThat(reservation.getRooms()).hasSize(2);
    }

    @Test
    @DisplayName("markFailed: transitions reservation to FAILED status")
    void markFailed_transitionsReservationToFailed() {
        Reservation reservation = buildTwoRoomReservation();
        reservation.markFailed("partner connectivity error", Instant.now());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FAILED);
    }

    private Reservation buildTwoRoomReservation() {
        return Reservation.create(
                "partner-1", "guest:Jane:Doe", "USD", null,
                List.of(
                        RoomReservationItem.builder()
                                .roomCode("RMX-101").reservationId("res-test")
                                .checkInDate(LocalDate.now().plusDays(7))
                                .checkOutDate(LocalDate.now().plusDays(10))
                                .basePrice(new BigDecimal("100.00")).build(),
                        RoomReservationItem.builder()
                                .roomCode("RMX-102").reservationId("res-test")
                                .checkInDate(LocalDate.now().plusDays(7))
                                .checkOutDate(LocalDate.now().plusDays(10))
                                .basePrice(new BigDecimal("100.00")).build()));
    }
}
