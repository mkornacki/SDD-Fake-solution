package com.acme.reservation.domain.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Asynchronous interaction unit for partner or payment system communication.
 * Tracks execution state and retry history.
 * Domain entity — no Spring or ORM dependencies.
 */
public class IntegrationTask {

    public enum TaskType {
        PARTNER_CREATE, PARTNER_CANCEL, REFUND_INITIATE
    }

    public enum TaskState {
        READY, RUNNING, RETRY_WAIT, SUCCEEDED, TERMINAL_FAILED;

        public boolean isTerminal() {
            return this == SUCCEEDED || this == TERMINAL_FAILED;
        }
    }

    public enum FailureClass {
        TRANSIENT, PERMANENT, NONE
    }

    private final String taskId;
    private final String reservationId;
    private final String roomItemId;
    private final TaskType taskType;
    private TaskState state;
    private int attemptCount;
    private final int maxAttempts;
    private Instant nextAttemptAt;
    private String lastFailureReason;
    private FailureClass failureClass;
    private final Instant createdAt;
    private Instant updatedAt;

    public IntegrationTask(String reservationId, String roomItemId,
            TaskType taskType, int maxAttempts) {
        this.taskId = UUID.randomUUID().toString();
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId required");
        this.roomItemId = roomItemId;
        this.taskType = Objects.requireNonNull(taskType, "taskType required");
        this.state = TaskState.READY;
        this.attemptCount = 0;
        this.maxAttempts = maxAttempts;
        this.failureClass = FailureClass.NONE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Reconstruction constructor
    public IntegrationTask(String taskId, String reservationId, String roomItemId,
            TaskType taskType, TaskState state, int attemptCount, int maxAttempts,
            Instant nextAttemptAt, String lastFailureReason, FailureClass failureClass,
            Instant createdAt, Instant updatedAt) {
        this.taskId = taskId;
        this.reservationId = reservationId;
        this.roomItemId = roomItemId;
        this.taskType = taskType;
        this.state = state;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastFailureReason = lastFailureReason;
        this.failureClass = failureClass;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void markRunning(Instant now) {
        this.state = TaskState.RUNNING;
        this.attemptCount++;
        this.updatedAt = now;
    }

    public void markSucceeded(Instant now) {
        this.state = TaskState.SUCCEEDED;
        this.updatedAt = now;
    }

    public void markRetryWait(String reason, Instant nextAttempt, Instant now) {
        this.state = TaskState.RETRY_WAIT;
        this.lastFailureReason = reason;
        this.failureClass = FailureClass.TRANSIENT;
        this.nextAttemptAt = nextAttempt;
        this.updatedAt = now;
    }

    public void markTerminalFailed(String reason, FailureClass fc, Instant now) {
        if (attemptCount >= maxAttempts || fc == FailureClass.PERMANENT) {
            this.state = TaskState.TERMINAL_FAILED;
            this.lastFailureReason = reason;
            this.failureClass = fc;
            this.updatedAt = now;
        }
    }

    public boolean isReadyForRetry(Instant now) {
        return state == TaskState.RETRY_WAIT
                && nextAttemptAt != null
                && !now.isBefore(nextAttemptAt);
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts
                && state != TaskState.TERMINAL_FAILED
                && state != TaskState.SUCCEEDED;
    }

    // --- Getters ---

    public String getTaskId() {
        return taskId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getRoomItemId() {
        return roomItemId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public TaskState getState() {
        return state;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public FailureClass getFailureClass() {
        return failureClass;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
