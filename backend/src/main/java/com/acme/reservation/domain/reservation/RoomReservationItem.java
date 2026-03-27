package com.acme.reservation.domain.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A single room booking within a reservation aggregate.
 * Independently cancellable within the parent Reservation.
 * Domain entity — no Spring or ORM dependencies.
 */
public class RoomReservationItem {

    public enum ProcessingStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED
    }

    private final String roomItemId;
    private final String reservationId;
    private final String roomCode;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private RoomStatus status;
    private final BigDecimal basePrice;
    private BigDecimal cancellationFee;
    private BigDecimal refundAmount;
    private String cancellationReason;
    private Instant cancelledAt;
    private ProcessingStatus processingStatus;

    private RoomReservationItem(Builder builder) {
        this.roomItemId = Objects.requireNonNull(builder.roomItemId, "roomItemId required");
        this.reservationId = Objects.requireNonNull(builder.reservationId, "reservationId required");
        this.roomCode = Objects.requireNonNull(builder.roomCode, "roomCode required");
        this.checkInDate = Objects.requireNonNull(builder.checkInDate, "checkInDate required");
        this.checkOutDate = Objects.requireNonNull(builder.checkOutDate, "checkOutDate required");
        this.status = Objects.requireNonNull(builder.status, "status required");
        this.basePrice = Objects.requireNonNull(builder.basePrice, "basePrice required");
        this.cancellationFee = builder.cancellationFee;
        this.refundAmount = builder.refundAmount;
        this.cancellationReason = builder.cancellationReason;
        this.cancelledAt = builder.cancelledAt;
        this.processingStatus = builder.processingStatus != null
                ? builder.processingStatus
                : ProcessingStatus.NOT_STARTED;
        validateDates();
        validateBasePrice();
    }

    private void validateDates() {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException(
                    "checkOutDate must be after checkInDate for room " + roomItemId);
        }
    }

    private void validateBasePrice() {
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("basePrice must be positive for room " + roomItemId);
        }
    }

    /**
     * Transitions the room to CANCELLATION_PENDING state.
     * Idempotent: if already cancelled, returns without error.
     */
    public void initiateCancellation(String reason, BigDecimal fee, BigDecimal refund, Instant at) {
        if (this.status == RoomStatus.CANCELLED) {
            return;
        }
        if (!this.status.canTransitionTo(RoomStatus.CANCELLATION_PENDING)) {
            throw new IllegalStateException(
                    "Cannot initiate cancellation from status " + this.status);
        }
        this.status = RoomStatus.CANCELLATION_PENDING;
        this.cancellationReason = reason;
        this.cancellationFee = fee;
        this.refundAmount = refund;
        this.processingStatus = ProcessingStatus.IN_PROGRESS;
        this.cancelledAt = at;
    }

    /**
     * Finalises cancellation to CANCELLED state.
     * Idempotent if already CANCELLED.
     */
    public void confirmCancellation(Instant at) {
        if (this.status == RoomStatus.CANCELLED) {
            return;
        }
        this.status = RoomStatus.CANCELLED;
        this.processingStatus = ProcessingStatus.COMPLETED;
        if (this.cancelledAt == null) {
            this.cancelledAt = at;
        }
    }

    public boolean isActive() {
        return status == RoomStatus.ACTIVE;
    }

    public boolean isCancelled() {
        return status == RoomStatus.CANCELLED;
    }

    // --- Getters ---

    public String getRoomItemId() {
        return roomItemId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String roomItemId;
        private String reservationId;
        private String roomCode;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private RoomStatus status = RoomStatus.ACTIVE;
        private BigDecimal basePrice;
        private BigDecimal cancellationFee;
        private BigDecimal refundAmount;
        private String cancellationReason;
        private Instant cancelledAt;
        private ProcessingStatus processingStatus;

        public Builder roomItemId(String v) {
            this.roomItemId = v;
            return this;
        }

        public Builder reservationId(String v) {
            this.reservationId = v;
            return this;
        }

        public Builder roomCode(String v) {
            this.roomCode = v;
            return this;
        }

        public Builder checkInDate(LocalDate v) {
            this.checkInDate = v;
            return this;
        }

        public Builder checkOutDate(LocalDate v) {
            this.checkOutDate = v;
            return this;
        }

        public Builder status(RoomStatus v) {
            this.status = v;
            return this;
        }

        public Builder basePrice(BigDecimal v) {
            this.basePrice = v;
            return this;
        }

        public Builder cancellationFee(BigDecimal v) {
            this.cancellationFee = v;
            return this;
        }

        public Builder refundAmount(BigDecimal v) {
            this.refundAmount = v;
            return this;
        }

        public Builder cancellationReason(String v) {
            this.cancellationReason = v;
            return this;
        }

        public Builder cancelledAt(Instant v) {
            this.cancelledAt = v;
            return this;
        }

        public Builder processingStatus(ProcessingStatus v) {
            this.processingStatus = v;
            return this;
        }

        public RoomReservationItem build() {
            if (roomItemId == null) {
                roomItemId = UUID.randomUUID().toString();
            }
            return new RoomReservationItem(this);
        }
    }
}
