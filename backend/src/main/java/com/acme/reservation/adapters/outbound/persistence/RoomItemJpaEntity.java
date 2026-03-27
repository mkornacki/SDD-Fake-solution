package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.reservation.RoomReservationItem;
import com.acme.reservation.domain.reservation.RoomStatus;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entity for the room_reservation_items table.
 */
@Entity
@Table(name = "room_reservation_items")
public class RoomItemJpaEntity {

    @Id
    @Column(name = "room_item_id")
    private String roomItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationJpaEntity reservation;

    @Column(name = "room_code", nullable = false)
    private String roomCode;

    @Column(name = "check_in_date", nullable = false)
    private String checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private String checkOutDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "cancellation_fee")
    private BigDecimal cancellationFee;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private String cancelledAt;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus;

    protected RoomItemJpaEntity() {}

    public static RoomItemJpaEntity from(RoomReservationItem domain, ReservationJpaEntity parent) {
        RoomItemJpaEntity entity = new RoomItemJpaEntity();
        entity.roomItemId = domain.getRoomItemId();
        entity.reservation = parent;
        entity.roomCode = domain.getRoomCode();
        entity.checkInDate = domain.getCheckInDate().toString();
        entity.checkOutDate = domain.getCheckOutDate().toString();
        entity.status = domain.getStatus().name();
        entity.basePrice = domain.getBasePrice();
        entity.cancellationFee = domain.getCancellationFee();
        entity.refundAmount = domain.getRefundAmount();
        entity.cancellationReason = domain.getCancellationReason();
        entity.cancelledAt = domain.getCancelledAt() != null
                ? domain.getCancelledAt().toString() : null;
        entity.processingStatus = domain.getProcessingStatus().name();
        return entity;
    }

    public RoomReservationItem toDomain() {
        return RoomReservationItem.builder()
                .roomItemId(roomItemId)
                .reservationId(reservation.getReservationId())
                .roomCode(roomCode)
                .checkInDate(LocalDate.parse(checkInDate))
                .checkOutDate(LocalDate.parse(checkOutDate))
                .status(RoomStatus.valueOf(status))
                .basePrice(basePrice)
                .cancellationFee(cancellationFee)
                .refundAmount(refundAmount)
                .cancellationReason(cancellationReason)
                .cancelledAt(cancelledAt != null ? Instant.parse(cancelledAt) : null)
                .processingStatus(RoomReservationItem.ProcessingStatus.valueOf(processingStatus))
                .build();
    }
}
