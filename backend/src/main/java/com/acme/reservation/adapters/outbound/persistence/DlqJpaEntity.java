package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.audit.DlqItem;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the dlq_items table.
 */
@Entity
@Table(name = "dlq_items")
public class DlqJpaEntity {

    @Id
    @Column(name = "dlq_id")
    private String dlqId;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Column(name = "failure_reason", nullable = false)
    private String failureReason;

    @Column(name = "attempt_history_ref", nullable = false)
    private String attemptHistoryRef;

    @Column(name = "masked_payload_ref", nullable = false)
    private String maskedPayloadRef;

    @Column(name = "replay_status", nullable = false)
    private String replayStatus;

    @Column(name = "replay_count", nullable = false)
    private int replayCount;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    protected DlqJpaEntity() {}

    public static DlqJpaEntity from(DlqItem domain) {
        DlqJpaEntity e = new DlqJpaEntity();
        e.dlqId = domain.getDlqId();
        e.taskId = domain.getTaskId();
        e.reservationId = domain.getReservationId();
        e.failureReason = domain.getFailureReason();
        e.attemptHistoryRef = domain.getAttemptHistoryRef();
        e.maskedPayloadRef = domain.getMaskedPayloadRef();
        e.replayStatus = domain.getReplayStatus().name();
        e.replayCount = domain.getReplayCount();
        e.createdAt = domain.getCreatedAt().toString();
        return e;
    }

    public DlqItem toDomain() {
        return new DlqItem(
                dlqId, taskId, reservationId, failureReason,
                attemptHistoryRef, maskedPayloadRef,
                DlqItem.ReplayStatus.valueOf(replayStatus),
                replayCount,
                Instant.parse(createdAt));
    }
}
