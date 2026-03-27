package com.acme.reservation.domain.financial;

import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.RoomReservationItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for computing FinancialBreakdown from a Reservation aggregate.
 * Pure logic — no Spring or ORM dependencies.
 */
public class FinancialCalculationService {

    /**
     * Builds a FinancialBreakdown that correctly reflects the current state
     * of the reservation's room items.
     */
    public FinancialBreakdown calculate(Reservation reservation) {
        List<FinancialLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalPenalties = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;

        for (RoomReservationItem room : reservation.getRooms()) {
            if (room.isActive()) {
                // Active rooms contribute to subtotal
                lineItems.add(new FinancialLineItem(
                        UUID.randomUUID().toString(),
                        FinancialLineItem.LineType.BASE_PRICE,
                        "Base price: " + room.getRoomCode(),
                        room.getBasePrice(),
                        room.getRoomItemId()));
                subtotal = subtotal.add(room.getBasePrice());
            } else if (room.isCancelled()) {
                // Only fully-confirmed cancellations contribute to penalties/refunds
                if (room.getCancellationFee() != null
                        && room.getCancellationFee().compareTo(BigDecimal.ZERO) > 0) {
                    lineItems.add(new FinancialLineItem(
                            UUID.randomUUID().toString(),
                            FinancialLineItem.LineType.CANCELLATION_PENALTY,
                            "Cancellation fee: " + room.getRoomCode(),
                            room.getCancellationFee(),
                            room.getRoomItemId()));
                    totalPenalties = totalPenalties.add(room.getCancellationFee());
                }
                if (room.getRefundAmount() != null
                        && room.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
                    lineItems.add(new FinancialLineItem(
                            UUID.randomUUID().toString(),
                            FinancialLineItem.LineType.REFUND,
                            "Refund: " + room.getRoomCode(),
                            room.getRefundAmount(),
                            room.getRoomItemId()));
                    totalRefunds = totalRefunds.add(room.getRefundAmount());
                }
            }
            // CANCELLATION_PENDING rooms are excluded from both subtotal and penalties
        }

        return new FinancialBreakdown(
                null,
                reservation.getReservationId(),
                lineItems,
                subtotal,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                totalPenalties,
                totalRefunds,
                Instant.now());
    }
}
