package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.application.ports.outbound.WorkItemRepository;
import com.acme.reservation.domain.replay.AsynchronousWorkItem;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA adapter for asynchronous work item persistence.
 */
@Repository
public class WorkItemJpaRepository implements WorkItemRepository {

    private final AsynchronousWorkItemSpringDataRepository springDataRepository;

    public WorkItemJpaRepository(AsynchronousWorkItemSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public AsynchronousWorkItem save(AsynchronousWorkItem workItem) {
        AsynchronousWorkItemJpaEntity saved =
                springDataRepository.save(AsynchronousWorkItemJpaEntity.from(workItem));
        return saved.toDomain();
    }

    @Override
    public Optional<AsynchronousWorkItem> findById(String workItemId) {
        return springDataRepository.findById(workItemId).map(AsynchronousWorkItemJpaEntity::toDomain);
    }

    @Override
    public List<AsynchronousWorkItem> findReady(Instant now) {
        return springDataRepository.findReadyItems().stream()
                .map(AsynchronousWorkItemJpaEntity::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<AsynchronousWorkItem> findForRetry(Instant now) {
        return springDataRepository.findRetryDueItems(now.toString()).stream()
                .map(AsynchronousWorkItemJpaEntity::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void updateState(
            String workItemId,
            AsynchronousWorkItem.State state,
            Instant nextAttemptAt,
            AsynchronousWorkItem.FailureClass failureClass,
            String lastFailureReason,
            Instant updatedAt) {
        Optional<AsynchronousWorkItemJpaEntity> entityOpt = springDataRepository.findById(workItemId);
        if (entityOpt.isEmpty()) {
            return;
        }

        AsynchronousWorkItemJpaEntity entity = entityOpt.get();
        AsynchronousWorkItem domain = entity.toDomain();
        AsynchronousWorkItem updated = new AsynchronousWorkItem(
                domain.getWorkItemId(),
                domain.getContextId(),
                domain.getScheduledAt(),
                domain.getMaxAttempts(),
                domain.getAttemptCount(),
                state,
                failureClass,
                nextAttemptAt,
                lastFailureReason,
                updatedAt);
        springDataRepository.save(AsynchronousWorkItemJpaEntity.from(updated));
    }
}
