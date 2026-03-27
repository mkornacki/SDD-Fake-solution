package com.acme.reservation.domain.financial;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Itemized financial summary for a reservation.
 * netTotal = subtotal + totalTax + totalFees + totalPenalties - totalRefunds
 * Domain entity — no Spring or ORM dependencies.
 */
public class FinancialBreakdown {

    private final String breakdownId;
    private final String reservationId;
    private final List<FinancialLineItem> lineItems;
    private final BigDecimal subtotal;
    private final BigDecimal totalTax;
    private final BigDecimal totalFees;
    private final BigDecimal totalPenalties;
    private final BigDecimal totalRefunds;
    private final BigDecimal netTotal;
    private final Instant calculatedAt;

    public FinancialBreakdown(String breakdownId, String reservationId,
            List<FinancialLineItem> lineItems, BigDecimal subtotal,
            BigDecimal totalTax, BigDecimal totalFees,
            BigDecimal totalPenalties, BigDecimal totalRefunds, Instant calculatedAt) {
        this.breakdownId = breakdownId != null ? breakdownId : UUID.randomUUID().toString();
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId required");
        this.lineItems = new ArrayList<>(lineItems != null ? lineItems : Collections.emptyList());
        this.subtotal = ifNull(subtotal);
        this.totalTax = ifNull(totalTax);
        this.totalFees = ifNull(totalFees);
        this.totalPenalties = ifNull(totalPenalties);
        this.totalRefunds = ifNull(totalRefunds);
        this.calculatedAt = calculatedAt != null ? calculatedAt : Instant.now();
        this.netTotal = this.subtotal
                .add(this.totalTax)
                .add(this.totalFees)
                .add(this.totalPenalties)
                .subtract(this.totalRefunds);
    }

    private static BigDecimal ifNull(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public String getBreakdownId() {
        return breakdownId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public List<FinancialLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public BigDecimal getTotalFees() {
        return totalFees;
    }

    public BigDecimal getTotalPenalties() {
        return totalPenalties;
    }

    public BigDecimal getTotalRefunds() {
        return totalRefunds;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }
}
