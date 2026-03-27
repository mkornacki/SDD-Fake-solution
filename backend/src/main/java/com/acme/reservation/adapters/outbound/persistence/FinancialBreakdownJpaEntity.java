package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.financial.FinancialBreakdown;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA entity for the financial_breakdowns table.
 */
@Entity
@Table(name = "financial_breakdowns")
public class FinancialBreakdownJpaEntity {

    @Id
    @Column(name = "breakdown_id")
    private String breakdownId;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "total_tax", nullable = false)
    private BigDecimal totalTax;

    @Column(name = "total_fees", nullable = false)
    private BigDecimal totalFees;

    @Column(name = "total_penalties", nullable = false)
    private BigDecimal totalPenalties;

    @Column(name = "total_refunds", nullable = false)
    private BigDecimal totalRefunds;

    @Column(name = "net_total", nullable = false)
    private BigDecimal netTotal;

    @Column(name = "calculated_at", nullable = false)
    private String calculatedAt;


    protected FinancialBreakdownJpaEntity() {}

    public static FinancialBreakdownJpaEntity from(FinancialBreakdown domain) {
        FinancialBreakdownJpaEntity e = new FinancialBreakdownJpaEntity();
        e.breakdownId = domain.getBreakdownId();
        e.reservationId = domain.getReservationId();
        e.subtotal = domain.getSubtotal();
        e.totalTax = domain.getTotalTax();
        e.totalFees = domain.getTotalFees();
        e.totalPenalties = domain.getTotalPenalties();
        e.totalRefunds = domain.getTotalRefunds();
        e.netTotal = domain.getNetTotal();
        e.calculatedAt = domain.getCalculatedAt().toString();
        return e;
    }

    public FinancialBreakdown toDomain(List<FinancialLineItemJpaEntity> lineItems) {
        return new FinancialBreakdown(
                breakdownId,
                reservationId,
                lineItems.stream().map(FinancialLineItemJpaEntity::toDomain).collect(Collectors.toList()),
                subtotal,
                totalTax,
                totalFees,
                totalPenalties,
                totalRefunds,
                Instant.parse(calculatedAt));
    }

    public String getBreakdownId() {
        return breakdownId;
    }

    public String getReservationId() {
        return reservationId;
    }
}
