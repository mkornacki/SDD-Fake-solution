package com.acme.reservation.domain.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Terminally failed integration task preserved for operator triage and replay.
 * Domain entity — no Spring or ORM dependencies.
 */
public final class DlqItem {

    public enum ReplayStatus {
        PENDING, IN_REVIEW, REPLAYED, CLOSED
    }

    private final String dlqId;
    private final String taskId;
    private final String reservationId;
    private final String failureReason;
    private final String attemptHistoryRef;
    private final String maskedPayloadRef;
    private ReplayStatus replayStatus;
    private int replayCount;
    private final Instant createdAt;

    public DlqItem(String taskId, String reservationId, String failureReason,
            String attemptHistoryRef, String maskedPayloadRef) {
        this.dlqId = UUID.randomUUID().toString();
        this.taskId = Objects.requireNonNull(taskId, "taskId required");
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId required");
        this.failureReason = Objects.requireNonNull(failureReason, "failureReason required");
        this.attemptHistoryRef = Objects.requireNonNull(attemptHistoryRef, "attemptHistoryRef required");
        this.maskedPayloadRef = Objects.requireNonNull(maskedPayloadRef, "maskedPayloadRef required");
        this.replayStatus = ReplayStatus.PENDING;
        this.replayCount = 0;
        this.createdAt = Instant.now();
    }

    // Reconstruction constructor
    public DlqItem(String dlqId, String taskId, String reservationId, String failureReason,
            String attemptHistoryRef, String maskedPayloadRef, ReplayStatus replayStatus,
            int replayCount, Instant createdAt) {
        this.dlqId = dlqId;
        this.taskId = taskId;
        this.reservationId = reservationId;
        this.failureReason = failureReason;
        this.attemptHistoryRef = attemptHistoryRef;
        this.maskedPayloadRef = maskedPayloadRef;
        this.replayStatus = replayStatus;
        this.replayCount = replayCount;
        this.createdAt = createdAt;
    }

    public void markInReview() {
        this.replayStatus = ReplayStatus.IN_REVIEW;
    }

    public void markReplayed() {
        this.replayStatus = ReplayStatus.REPLAYED;
        this.replayCount++;
    }

    public void close() {
        this.replayStatus = ReplayStatus.CLOSED;
    }

    public String getDlqId() {
        return dlqId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getAttemptHistoryRef() {
        return attemptHistoryRef;
    }

    public String getMaskedPayloadRef() {
        return maskedPayloadRef;
    }

    public ReplayStatus getReplayStatus() {
        return replayStatus;
    }

    public int getReplayCount() {
        return replayCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
