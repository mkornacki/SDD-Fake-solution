package com.acme.reservation.domain.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Reservation aggregate root.
 * Owns the lifecycle of a multi-room booking, enforces state transitions,
 * and maintains financial consistency across its room items.
 * Domain entity — no Spring or ORM dependencies.
 */
public class Reservation {

    private final String reservationId;
    private final String partnerId;
    private String externalReference;
    private ReservationStatus status;
    private final String currencyCode;
    private BigDecimal totalPrice;
    private BigDecimal totalRefundAmount;
    private BigDecimal totalCancellationFee;
    private int roomCount;
    private int version;
    private final Instant createdAt;
    private Instant updatedAt;
    private final String guestId;
    private final List<RoomReservationItem> rooms;

    private Reservation(Builder builder) {
        this.reservationId = Objects.requireNonNull(builder.reservationId, "reservationId required");
        this.partnerId = Objects.requireNonNull(builder.partnerId, "partnerId required");
        this.externalReference = builder.externalReference;
        this.status = builder.status != null ? builder.status : ReservationStatus.PENDING;
        this.currencyCode = Objects.requireNonNull(builder.currencyCode, "currencyCode required");
        this.totalPrice = builder.totalPrice != null ? builder.totalPrice : BigDecimal.ZERO;
        this.totalRefundAmount = builder.totalRefundAmount != null
                ? builder.totalRefundAmount : BigDecimal.ZERO;
        this.totalCancellationFee = builder.totalCancellationFee != null
                ? builder.totalCancellationFee : BigDecimal.ZERO;
        this.version = builder.version;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : this.createdAt;
        this.guestId = Objects.requireNonNull(builder.guestId, "guestId required");
        this.rooms = new ArrayList<>(builder.rooms != null ? builder.rooms : Collections.emptyList());
        this.roomCount = (int) rooms.stream().filter(r -> !r.isCancelled()).count();
        recalculateTotals();
    }

    /**
     * Factory: create a new PENDING reservation from a list of rooms.
     * Enforces: at least one room, consistent totals.
     */
    public static Reservation create(
            String partnerId,
            String guestId,
            String currencyCode,
            String externalReference,
            List<RoomReservationItem> rooms) {

        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("Reservation must contain at least one room");
        }
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new Reservation(new Builder()
                .reservationId(id)
                .partnerId(partnerId)
                .guestId(guestId)
                .currencyCode(currencyCode)
                .externalReference(externalReference)
                .status(ReservationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .rooms(rooms));
    }

    /**
     * Transitions the reservation to ACTIVE status.
     */
    public void activate() {
        transitionTo(ReservationStatus.ACTIVE);
    }

    /**
     * Cancels one room item within this reservation.
     * Recalculates totals and updates the reservation status.
     */
    public void cancelRoom(String roomItemId, String reason,
            BigDecimal fee, BigDecimal refund, Instant at) {
        RoomReservationItem room = findRoomById(roomItemId);
        room.initiateCancellation(reason, fee, refund, at);
        recalculateStatusAfterRoomChange();
        recalculateTotals();
        this.updatedAt = at;
    }

    /**
     * Confirms a room cancellation is complete (partner ACK received).
     */
    public void confirmRoomCancellation(String roomItemId, Instant at) {
        RoomReservationItem room = findRoomById(roomItemId);
        room.confirmCancellation(at);
        recalculateStatusAfterRoomChange();
        recalculateTotals();
        this.updatedAt = at;
    }

    /**
     * Cancels all active rooms in this reservation.
     * Idempotent: already-cancelled rooms are skipped.
     */
    public void cancelAll(String reason, BigDecimal feePerRoom,
            BigDecimal refundPerRoom, Instant at) {
        for (RoomReservationItem room : rooms) {
            if (room.isActive()) {
                room.initiateCancellation(reason, feePerRoom, refundPerRoom, at);
            }
        }
        transitionTo(ReservationStatus.CANCELLED);
        recalculateTotals();
        this.updatedAt = at;
    }

    /**
     * Marks the reservation as FAILED.
     */
    public void markFailed(Instant at) {
        transitionTo(ReservationStatus.FAILED);
        this.updatedAt = at;
    }

    /**
     * Marks the reservation as FAILED with a reason.
     */
    public void markFailed(String reason, Instant at) {
        transitionTo(ReservationStatus.FAILED);
        this.updatedAt = at;
    }

    private void transitionTo(ReservationStatus next) {
        if (this.status == next) {
            return;
        }
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Cannot transition reservation " + reservationId
                    + " from " + this.status + " to " + next);
        }
        this.status = next;
    }

    private void recalculateStatusAfterRoomChange() {
        long activeCount = rooms.stream().filter(RoomReservationItem::isActive).count();
        long nonActiveCount = rooms.size() - activeCount; // includes CANCELLATION_PENDING + CANCELLED

        if (nonActiveCount == 0) {
            return;
        } else if (activeCount == 0) {
            if (this.status != ReservationStatus.CANCELLED) {
                transitionTo(ReservationStatus.CANCELLED);
            }
        } else {
            if (this.status == ReservationStatus.ACTIVE) {
                transitionTo(ReservationStatus.PARTIALLY_CANCELLED);
            }
            // PENDING stays PENDING when rooms are partially cancelled
        }
        this.roomCount = (int) activeCount;
    }

    private void recalculateTotals() {
        this.totalPrice = rooms.stream()
                .filter(RoomReservationItem::isActive)
                .map(RoomReservationItem::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalCancellationFee = rooms.stream()
                .filter(r -> !r.isActive())
                .map(r -> r.getCancellationFee() != null ? r.getCancellationFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalRefundAmount = rooms.stream()
                .filter(r -> !r.isActive())
                .map(r -> r.getRefundAmount() != null ? r.getRefundAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.roomCount = (int) rooms.stream().filter(RoomReservationItem::isActive).count();
    }

    private RoomReservationItem findRoomById(String roomItemId) {
        return rooms.stream()
                .filter(r -> r.getRoomItemId().equals(roomItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room item " + roomItemId + " not found in reservation " + reservationId));
    }

    // --- Getters ---

    public String getReservationId() {
        return reservationId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BigDecimal getTotalRefundAmount() {
        return totalRefundAmount;
    }

    public BigDecimal getTotalCancellationFee() {
        return totalCancellationFee;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getGuestId() {
        return guestId;
    }

    public List<RoomReservationItem> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String reservationId;
        private String partnerId;
        private String externalReference;
        private ReservationStatus status;
        private String currencyCode;
        private BigDecimal totalPrice;
        private BigDecimal totalRefundAmount;
        private BigDecimal totalCancellationFee;
        private int version;
        private Instant createdAt;
        private Instant updatedAt;
        private String guestId;
        private List<RoomReservationItem> rooms;

        public Builder reservationId(String v) {
            this.reservationId = v;
            return this;
        }

        public Builder partnerId(String v) {
            this.partnerId = v;
            return this;
        }

        public Builder externalReference(String v) {
            this.externalReference = v;
            return this;
        }

        public Builder status(ReservationStatus v) {
            this.status = v;
            return this;
        }

        public Builder currencyCode(String v) {
            this.currencyCode = v;
            return this;
        }

        public Builder totalPrice(BigDecimal v) {
            this.totalPrice = v;
            return this;
        }

        public Builder totalRefundAmount(BigDecimal v) {
            this.totalRefundAmount = v;
            return this;
        }

        public Builder totalCancellationFee(BigDecimal v) {
            this.totalCancellationFee = v;
            return this;
        }

        public Builder version(int v) {
            this.version = v;
            return this;
        }

        public Builder createdAt(Instant v) {
            this.createdAt = v;
            return this;
        }

        public Builder updatedAt(Instant v) {
            this.updatedAt = v;
            return this;
        }

        public Builder guestId(String v) {
            this.guestId = v;
            return this;
        }

        public Builder rooms(List<RoomReservationItem> v) {
            this.rooms = v;
            return this;
        }

        public Reservation build() {
            if (reservationId == null) {
                reservationId = UUID.randomUUID().toString();
            }
            return new Reservation(this);
        }
    }
}
