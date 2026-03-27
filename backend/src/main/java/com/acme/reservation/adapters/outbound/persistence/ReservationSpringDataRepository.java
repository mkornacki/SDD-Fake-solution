package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for ReservationJpaEntity.
 */
interface ReservationSpringDataRepository extends JpaRepository<ReservationJpaEntity, String> {
}
