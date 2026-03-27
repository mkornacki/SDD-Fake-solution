package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.reservation.ReservationRequestContext;
import java.util.Optional;

/**
 * Outbound port for reservation request context persistence.
 */
public interface RequestContextRepository {

  /**
   * Save or update a request context.
   */
  ReservationRequestContext save(ReservationRequestContext context);

  /**
   * Find a request context by its contextId.
   */
  Optional<ReservationRequestContext> findByContextId(String contextId);

  /**
   * Update the status of a request context.
   */
  void updateStatus(String contextId, ReservationRequestContext.Status status);
}
