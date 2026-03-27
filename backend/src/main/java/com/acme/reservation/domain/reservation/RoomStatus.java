package com.acme.reservation.domain.reservation;

/**
 * Lifecycle states for an individual room reservation item.
 * State transitions are enforced by the RoomReservationItem entity.
 */
public enum RoomStatus {
    ACTIVE,
    CANCELLATION_PENDING,
    CANCELLED;

    public boolean isTerminal() {
        return this == CANCELLED;
    }

    public boolean canTransitionTo(RoomStatus next) {
        switch (this) {
            case ACTIVE:
                return next == CANCELLATION_PENDING || next == CANCELLED;
            case CANCELLATION_PENDING:
                return next == CANCELLED || next == ACTIVE;
            default:
                return false;
        }
    }
}
