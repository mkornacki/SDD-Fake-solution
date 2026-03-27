package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for DlqJpaEntity.
 * Extracted as top-level to ensure discovery by Spring Data JPA scanning.
 */
interface DlqSpringDataRepository extends JpaRepository<DlqJpaEntity, String> {
}
