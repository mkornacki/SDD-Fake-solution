package com.acme.reservation.domain;

import com.acme.reservation.domain.replay.AsynchronousWorkItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T021: AsynchronousWorkItem state transitions")
class WorkItemStateTest {

    @Test
    @DisplayName("READY -> RUNNING -> SUCCEEDED happy path")
    void stateTransitions_happyPath() {
        AsynchronousWorkItem item = AsynchronousWorkItem.create("ctx-1", 5, Instant.now());

        assertThat(item.getState()).isEqualTo(AsynchronousWorkItem.State.READY);

        item.markRunning(Instant.now());
        assertThat(item.getState()).isEqualTo(AsynchronousWorkItem.State.RUNNING);
        assertThat(item.getAttemptCount()).isEqualTo(1);

        item.markSucceeded(Instant.now());
        assertThat(item.getState()).isEqualTo(AsynchronousWorkItem.State.SUCCEEDED);
        assertThat(item.getState().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Transient failure moves RUNNING -> RETRY_WAIT with nextAttemptAt")
    void transientFailure_movesToRetryWait() {
        AsynchronousWorkItem item = AsynchronousWorkItem.create("ctx-1", 5, Instant.now());
        item.markRunning(Instant.now());

        Instant now = Instant.now();
        item.markTransientFailure("timeout", now, 1, 2.0, 60, false);

        assertThat(item.getState()).isEqualTo(AsynchronousWorkItem.State.RETRY_WAIT);
        assertThat(item.getFailureClass()).isEqualTo(AsynchronousWorkItem.FailureClass.TRANSIENT);
        assertThat(item.getNextAttemptAt()).isAfter(now);
    }

    @Test
    @DisplayName("Permanent failure moves RUNNING -> TERMINAL_FAILED")
    void permanentFailure_becomesTerminalFailed() {
        AsynchronousWorkItem item = AsynchronousWorkItem.create("ctx-1", 5, Instant.now());
        item.markRunning(Instant.now());

        item.markPermanentFailure("validation error", Instant.now());

        assertThat(item.getState()).isEqualTo(AsynchronousWorkItem.State.TERMINAL_FAILED);
        assertThat(item.getFailureClass()).isEqualTo(AsynchronousWorkItem.FailureClass.PERMANENT);
        assertThat(item.getState().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Exhausted attempts transitions to TERMINAL_FAILED")
    void maxAttemptsExhausted_becomesTerminalFailed() {
        AsynchronousWorkItem item = AsynchronousWorkItem.create("ctx-1", 2, Instant.now());

        item.markRunning(Instant.now());
        item.markTransientFailure("503", Instant.now(), 1, 2.0, 60, false);
        item.markReadyForRetry(Instant.now());

        item.markRunning(Instant.now());
        item.markTransientFailure("503 again", Instant.now(), 1, 2.0, 60, false);

        assertThat(item.getState()).isEqualTo(AsynchronousWorkItem.State.TERMINAL_FAILED);
        assertThat(item.getFailureClass()).isEqualTo(AsynchronousWorkItem.FailureClass.TRANSIENT);
        assertThat(item.canRetry()).isFalse();
    }

    @Test
    @DisplayName("Failure classification: 5xx transient, 4xx permanent")
    void classifyFailure_httpStatusBased() {
        assertThat(AsynchronousWorkItem.classifyHttpStatus(503))
                .isEqualTo(AsynchronousWorkItem.FailureClass.TRANSIENT);
        assertThat(AsynchronousWorkItem.classifyHttpStatus(429))
                .isEqualTo(AsynchronousWorkItem.FailureClass.TRANSIENT);
        assertThat(AsynchronousWorkItem.classifyHttpStatus(400))
                .isEqualTo(AsynchronousWorkItem.FailureClass.PERMANENT);
    }
}
