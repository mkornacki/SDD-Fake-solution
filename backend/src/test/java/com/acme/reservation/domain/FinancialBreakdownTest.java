package com.acme.reservation.domain;

import com.acme.reservation.domain.financial.FinancialBreakdown;
import com.acme.reservation.domain.financial.FinancialCalculationService;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T036: Unit tests for FinancialBreakdown recalculation after room cancellation.
 * Verifies totals, penalties, refunds, and netTotal formula.
 */
@DisplayName("T036: FinancialBreakdown recalculation tests")
class FinancialBreakdownTest {

    private final FinancialCalculationService calculationService = new FinancialCalculationService();

    @Test
    @DisplayName("FinancialBreakdown: netTotal = subtotal + tax + fees + penalties - refunds")
    void financialBreakdown_netTotalFormula_correct() {
        FinancialBreakdown bd = new FinancialBreakdown(
                null, "res-1", List.of(),
                new BigDecimal("200.00"), // subtotal
                new BigDecimal("20.00"),  // tax
                new BigDecimal("10.00"),  // fees
                new BigDecimal("30.00"),  // penalties
                new BigDecimal("50.00"),  // refunds
                Instant.now());

        // netTotal = 200 + 20 + 10 + 30 - 50 = 210
        assertThat(bd.getNetTotal()).isEqualByComparingTo(new BigDecimal("210.00"));
    }

    @Test
    @DisplayName("FinancialCalculationService: calculates subtotal from active rooms")
    void calculateService_subtotal_fromActiveRooms() {
        Reservation reservation = buildReservationTwoRooms();

        FinancialBreakdown bd = calculationService.calculate(reservation);

        assertThat(bd.getSubtotal()).isEqualByComparingTo(new BigDecimal("300.00")); // 2 x 150
        assertThat(bd.getTotalPenalties()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bd.getTotalRefunds()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("FinancialCalculationService: recalculates penalties and refunds after room cancellation")
    void calculateService_recalculates_afterRoomCancellation() {
        Reservation reservation = buildReservationTwoRooms();
        reservation.activate();

        // Cancel one room
        String roomId = reservation.getRooms().get(0).getRoomItemId();
        reservation.cancelRoom(roomId, "no show",
                new BigDecimal("20.00"), new BigDecimal("80.00"), Instant.now());

        FinancialBreakdown bd = calculationService.calculate(reservation);

        // One active room (150), one in cancellation with penalty 20 and refund 80
        assertThat(bd.getSubtotal()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(bd.getTotalPenalties()).isEqualByComparingTo(BigDecimal.ZERO); // CANCELLATION_PENDING not yet CANCELLED
        // After confirm cancellation
        reservation.confirmRoomCancellation(roomId, Instant.now());
        FinancialBreakdown bd2 = calculationService.calculate(reservation);
        assertThat(bd2.getTotalPenalties()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(bd2.getTotalRefunds()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("FinancialBreakdown: lineItems tracks each room contribution")
    void calculateService_lineItems_tracked() {
        Reservation reservation = buildReservationTwoRooms();
        FinancialBreakdown bd = calculationService.calculate(reservation);

        assertThat(bd.getLineItems()).hasSize(2);
    }

    private Reservation buildReservationTwoRooms() {
        return Reservation.create(
                "partner-1",
                "guest:Jane:Doe",
                "USD",
                null,
                List.of(
                        RoomReservationItem.builder()
                                .roomCode("RMX-101")
                                .reservationId("res-test")
                                .checkInDate(LocalDate.now().plusDays(7))
                                .checkOutDate(LocalDate.now().plusDays(10))
                                .basePrice(new BigDecimal("150.00"))
                                .build(),
                        RoomReservationItem.builder()
                                .roomCode("RMX-102")
                                .reservationId("res-test")
                                .checkInDate(LocalDate.now().plusDays(7))
                                .checkOutDate(LocalDate.now().plusDays(10))
                                .basePrice(new BigDecimal("150.00"))
                                .build()));
    }
}
