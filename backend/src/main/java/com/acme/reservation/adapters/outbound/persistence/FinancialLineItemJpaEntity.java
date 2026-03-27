package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.financial.FinancialLineItem;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.math.BigDecimal;

/**
 * JPA entity for the financial_line_items table.
 */
@Entity
@Table(name = "financial_line_items")
public class FinancialLineItemJpaEntity {

    @Id
    @Column(name = "line_item_id")
    private String lineItemId;

    @Column(name = "breakdown_id", nullable = false)
    private String breakdownId;

    @Column(name = "line_type", nullable = false)
    private String lineType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "room_item_id")
    private String roomItemId;

    protected FinancialLineItemJpaEntity() {}

    public static FinancialLineItemJpaEntity from(FinancialLineItem item, String breakdownId) {
        FinancialLineItemJpaEntity e = new FinancialLineItemJpaEntity();
        e.lineItemId = item.getLineItemId();
        e.breakdownId = breakdownId;
        e.lineType = item.getLineType().name();
        e.description = item.getDescription();
        e.amount = item.getAmount();
        e.roomItemId = item.getRoomItemId();
        return e;
    }

    public FinancialLineItem toDomain() {
        return new FinancialLineItem(
                lineItemId,
                FinancialLineItem.LineType.valueOf(lineType),
                description,
                amount,
                roomItemId);
    }
}
