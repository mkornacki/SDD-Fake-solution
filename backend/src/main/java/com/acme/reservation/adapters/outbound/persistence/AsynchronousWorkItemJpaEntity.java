package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.replay.AsynchronousWorkItem;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;

/**
 * JPA entity mapping for asynchronous_work_items table.
 */
@Entity
@Table(name = "asynchronous_work_items")
public class AsynchronousWorkItemJpaEntity {

    @Id
    @Column(name = "work_item_id")
    private String workItemId;

    @Column(name = "context_id", nullable = false)
    private String contextId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at")
    private String nextAttemptAt;

    @Column(name = "failure_class", nullable = false)
    private String failureClass;

    @Column(name = "last_failure_reason")
    private String lastFailureReason;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    protected AsynchronousWorkItemJpaEntity() {
    }

    public static AsynchronousWorkItemJpaEntity from(AsynchronousWorkItem domain) {
        AsynchronousWorkItemJpaEntity entity = new AsynchronousWorkItemJpaEntity();
        entity.workItemId = domain.getWorkItemId();
        entity.contextId = domain.getContextId();
        entity.status = domain.getState().name();
        entity.attemptCount = domain.getAttemptCount();
        entity.maxAttempts = domain.getMaxAttempts();
        entity.nextAttemptAt = domain.getNextAttemptAt() != null ? domain.getNextAttemptAt().toString() : null;
        entity.failureClass = domain.getFailureClass().name();
        entity.lastFailureReason = domain.getLastFailureReason();
        entity.createdAt = domain.getScheduledAt().toString();
        entity.updatedAt = domain.getUpdatedAt().toString();
        return entity;
    }

    public AsynchronousWorkItem toDomain() {
        return new AsynchronousWorkItem(
                workItemId,
                contextId,
                Instant.parse(createdAt),
                maxAttempts,
                attemptCount,
                AsynchronousWorkItem.State.valueOf(status),
                AsynchronousWorkItem.FailureClass.valueOf(failureClass),
                nextAttemptAt != null ? Instant.parse(nextAttemptAt) : null,
                lastFailureReason,
                Instant.parse(updatedAt));
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public String getStatus() {
        return status;
    }

    public String getNextAttemptAt() {
        return nextAttemptAt;
    }
}
