package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import com.acme.reservation.domain.reservation.ReservationRequestContext;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter for ReservationRequestContext persistence.
 * Implements the RequestContextRepository outbound port.
 */
@Repository
public class RequestContextJpaRepository implements RequestContextRepository {

  private final RequestContextSpringDataRepository springDataRepo;

  public RequestContextJpaRepository(RequestContextSpringDataRepository springDataRepo) {
    this.springDataRepo = springDataRepo;
  }

  /**
   * Save or update a request context.
   */
  @Override
  public ReservationRequestContext save(ReservationRequestContext context) {
    RequestContextJpaEntity entity = RequestContextJpaEntity.from(context);
    RequestContextJpaEntity saved = springDataRepo.save(entity);
    return saved.toDomain();
  }

  /**
   * Find a request context by its contextId.
   */
  @Override
  public Optional<ReservationRequestContext> findByContextId(String contextId) {
    return springDataRepo.findById(contextId).map(RequestContextJpaEntity::toDomain);
  }

  /**
   * Update the status of a request context.
   */
  @Override
  public void updateStatus(String contextId, ReservationRequestContext.Status status) {
    Optional<RequestContextJpaEntity> entity = springDataRepo.findById(contextId);
    if (entity.isPresent()) {
      RequestContextJpaEntity e = entity.get();
      e.setStatus(status.name());
      e.setUpdatedAt(java.time.Instant.now());
      springDataRepo.save(e);
    }
  }
}
