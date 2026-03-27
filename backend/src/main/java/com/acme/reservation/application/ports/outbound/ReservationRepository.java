package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.reservation.Reservation;

import java.util.Optional;

/**
 * Outbound port for reservation persistence.
 */
public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(String reservationId);
}
