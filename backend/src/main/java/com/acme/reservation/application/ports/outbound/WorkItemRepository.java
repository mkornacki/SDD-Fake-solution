package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.replay.AsynchronousWorkItem;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for asynchronous work item persistence.
 */
public interface WorkItemRepository {

    AsynchronousWorkItem save(AsynchronousWorkItem workItem);

    Optional<AsynchronousWorkItem> findById(String workItemId);

    List<AsynchronousWorkItem> findReady(Instant now);

    List<AsynchronousWorkItem> findForRetry(Instant now);

    void updateState(
            String workItemId,
            AsynchronousWorkItem.State state,
            Instant nextAttemptAt,
            AsynchronousWorkItem.FailureClass failureClass,
            String lastFailureReason,
            Instant updatedAt);
}
