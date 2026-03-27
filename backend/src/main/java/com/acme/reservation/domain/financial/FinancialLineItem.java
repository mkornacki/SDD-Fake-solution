package com.acme.reservation.domain.financial;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single line item within a financial breakdown.
 * Domain entity — no Spring or ORM dependencies.
 */
public final class FinancialLineItem {

    public enum LineType {
        BASE_PRICE, TAX, FEE, CANCELLATION_PENALTY, REFUND
    }

    private final String lineItemId;
    private final LineType lineType;
    private final String description;
    private final BigDecimal amount;
    private final String roomItemId;

    public FinancialLineItem(String lineItemId, LineType lineType, String description,
            BigDecimal amount, String roomItemId) {
        this.lineItemId = Objects.requireNonNull(lineItemId, "lineItemId required");
        this.lineType = Objects.requireNonNull(lineType, "lineType required");
        this.description = Objects.requireNonNull(description, "description required");
        this.amount = Objects.requireNonNull(amount, "amount required");
        this.roomItemId = roomItemId;
    }

    public String getLineItemId() {
        return lineItemId;
    }

    public LineType getLineType() {
        return lineType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRoomItemId() {
        return roomItemId;
    }
}
