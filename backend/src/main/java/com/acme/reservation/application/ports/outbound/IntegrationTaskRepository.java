package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.audit.IntegrationTask;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for integration task persistence.
 */
public interface IntegrationTaskRepository {
    IntegrationTask save(IntegrationTask task);

    Optional<IntegrationTask> findById(String taskId);

    List<IntegrationTask> findPendingTasks();
}
