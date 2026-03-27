package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.audit.IntegrationTask;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the integration_tasks table.
 */
@Entity
@Table(name = "integration_tasks")
public class IntegrationTaskJpaEntity {

    @Id
    @Column(name = "task_id")
    private String taskId;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "room_item_id")
    private String roomItemId;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(nullable = false)
    private String state;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at")
    private String nextAttemptAt;

    @Column(name = "last_failure_reason")
    private String lastFailureReason;

    @Column(name = "failure_class", nullable = false)
    private String failureClass;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    protected IntegrationTaskJpaEntity() {}

    public static IntegrationTaskJpaEntity from(IntegrationTask domain) {
        IntegrationTaskJpaEntity e = new IntegrationTaskJpaEntity();
        e.taskId = domain.getTaskId();
        e.reservationId = domain.getReservationId();
        e.roomItemId = domain.getRoomItemId();
        e.taskType = domain.getTaskType().name();
        e.state = domain.getState().name();
        e.attemptCount = domain.getAttemptCount();
        e.maxAttempts = domain.getMaxAttempts();
        e.nextAttemptAt = domain.getNextAttemptAt() != null
                ? domain.getNextAttemptAt().toString() : null;
        e.lastFailureReason = domain.getLastFailureReason();
        e.failureClass = domain.getFailureClass().name();
        e.createdAt = domain.getCreatedAt().toString();
        e.updatedAt = domain.getUpdatedAt().toString();
        return e;
    }

    public IntegrationTask toDomain() {
        return new IntegrationTask(
                taskId, reservationId, roomItemId,
                IntegrationTask.TaskType.valueOf(taskType),
                IntegrationTask.TaskState.valueOf(state),
                attemptCount, maxAttempts,
                nextAttemptAt != null ? Instant.parse(nextAttemptAt) : null,
                lastFailureReason,
                IntegrationTask.FailureClass.valueOf(failureClass),
                Instant.parse(createdAt),
                Instant.parse(updatedAt));
    }

    public String getState() {
        return state;
    }
}
