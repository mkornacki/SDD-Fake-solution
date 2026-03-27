package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter for reservation persistence.
 * Uses @OneToMany cascade in ReservationJpaEntity — JPA handles correct INSERT order.
 */
@Repository
public class ReservationJpaRepository implements ReservationRepository {

    private final ReservationSpringDataRepository springDataRepo;

    public ReservationJpaRepository(ReservationSpringDataRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity entity = ReservationJpaEntity.from(reservation);
        ReservationJpaEntity saved = springDataRepo.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return springDataRepo.findById(reservationId)
                .map(ReservationJpaEntity::toDomain);
    }

}
