package com.acme.reservation.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for RequestContextJpaEntity.
 */
@Repository
public interface RequestContextSpringDataRepository extends JpaRepository<RequestContextJpaEntity, String> {
}
