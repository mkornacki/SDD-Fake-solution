package com.acme.reservation.domain.reservation;

/**
 * Lifecycle states for a reservation aggregate.
 * State transitions are enforced by the Reservation domain entity.
 */
public enum ReservationStatus {
    PENDING,
    ACTIVE,
    PARTIALLY_CANCELLED,
    CANCELLED,
    FAILED;

    public boolean isTerminal() {
        return this == CANCELLED || this == FAILED;
    }

    public boolean canTransitionTo(ReservationStatus next) {
        switch (this) {
            case PENDING:
                return next == ACTIVE || next == FAILED || next == CANCELLED;
            case ACTIVE:
                return next == PARTIALLY_CANCELLED || next == CANCELLED || next == FAILED;
            case PARTIALLY_CANCELLED:
                return next == CANCELLED || next == FAILED;
            default:
                return false;
        }
    }
}
