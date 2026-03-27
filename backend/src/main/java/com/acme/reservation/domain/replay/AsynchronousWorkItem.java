package com.acme.reservation.domain.replay;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing durable async partner-processing work.
 */
public class AsynchronousWorkItem {

    public enum State {
        READY,
        RUNNING,
        RETRY_WAIT,
        SUCCEEDED,
        TERMINAL_FAILED;

        public boolean isTerminal() {
            return this == SUCCEEDED || this == TERMINAL_FAILED;
        }
    }

    public enum FailureClass {
        TRANSIENT,
        PERMANENT,
        NONE
    }

    private final String workItemId;
    private final String contextId;
    private final Instant scheduledAt;
    private final int maxAttempts;

    private int attemptCount;
    private State state;
    private FailureClass failureClass;
    private Instant nextAttemptAt;
    private String lastFailureReason;
    private Instant updatedAt;

    private AsynchronousWorkItem(String workItemId, String contextId, int maxAttempts, Instant scheduledAt) {
        this.workItemId = workItemId;
        this.contextId = Objects.requireNonNull(contextId, "contextId is required");
        this.maxAttempts = maxAttempts;
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt is required");
        this.attemptCount = 0;
        this.state = State.READY;
        this.failureClass = FailureClass.NONE;
        this.updatedAt = scheduledAt;
    }

    public AsynchronousWorkItem(
            String workItemId,
            String contextId,
            Instant scheduledAt,
            int maxAttempts,
            int attemptCount,
            State state,
            FailureClass failureClass,
            Instant nextAttemptAt,
            String lastFailureReason,
            Instant updatedAt) {
        this.workItemId = workItemId;
        this.contextId = Objects.requireNonNull(contextId, "contextId is required");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt is required");
        this.maxAttempts = maxAttempts;
        this.attemptCount = attemptCount;
        this.state = state;
        this.failureClass = failureClass;
        this.nextAttemptAt = nextAttemptAt;
        this.lastFailureReason = lastFailureReason;
        this.updatedAt = updatedAt;
    }

    public static AsynchronousWorkItem create(String contextId, int maxAttempts, Instant scheduledAt) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        return new AsynchronousWorkItem(UUID.randomUUID().toString(), contextId, maxAttempts, scheduledAt);
    }

    public void markRunning(Instant now) {
        if (state != State.READY) {
            throw new IllegalStateException("Only READY work items can start running");
        }
        this.state = State.RUNNING;
        this.attemptCount += 1;
        this.updatedAt = now;
    }

    public void markSucceeded(Instant now) {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Only RUNNING work items can succeed");
        }
        this.state = State.SUCCEEDED;
        this.updatedAt = now;
    }

    public void markTransientFailure(
            String reason,
            Instant now,
            long baseSeconds,
            double multiplier,
            long maxSeconds,
            boolean jitter) {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Only RUNNING work items can fail");
        }

        this.lastFailureReason = reason;
        this.failureClass = FailureClass.TRANSIENT;
        this.updatedAt = now;

        if (attemptCount >= maxAttempts) {
            this.state = State.TERMINAL_FAILED;
            this.nextAttemptAt = null;
            return;
        }

        Duration delay = RetryBackoffCalculator.calculate(attemptCount, baseSeconds, multiplier, maxSeconds, jitter);
        this.state = State.RETRY_WAIT;
        this.nextAttemptAt = now.plus(delay);
    }

    public void markReadyForRetry(Instant now) {
        if (state != State.RETRY_WAIT) {
            throw new IllegalStateException("Only RETRY_WAIT work items can be re-queued");
        }
        this.state = State.READY;
        this.nextAttemptAt = null;
        this.updatedAt = now;
    }

    public void markPermanentFailure(String reason, Instant now) {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Only RUNNING work items can fail");
        }
        this.state = State.TERMINAL_FAILED;
        this.failureClass = FailureClass.PERMANENT;
        this.lastFailureReason = reason;
        this.updatedAt = now;
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts && !state.isTerminal();
    }

    public static FailureClass classifyHttpStatus(int httpStatus) {
        if (httpStatus == 429 || httpStatus >= 500) {
            return FailureClass.TRANSIENT;
        }
        if (httpStatus >= 400 && httpStatus < 500) {
            return FailureClass.PERMANENT;
        }
        return FailureClass.NONE;
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public String getContextId() {
        return contextId;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
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

    public State getState() {
        return state;
    }

    public FailureClass getFailureClass() {
        return failureClass;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
