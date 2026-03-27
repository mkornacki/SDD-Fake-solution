package com.acme.reservation.domain;

import com.acme.reservation.domain.audit.IntegrationTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T044: Unit tests for IntegrationTask state machine.
 * Verifies READY→RUNNING→SUCCEEDED and retry/DLQ paths.
 */
@DisplayName("T044: IntegrationTask state machine tests")
class IntegrationTaskStateTest {

    @Test
    @DisplayName("READY→RUNNING→SUCCEEDED happy path")
    void task_happyPath_readyToSucceeded() {
        IntegrationTask task = new IntegrationTask(
                "res-001", null, IntegrationTask.TaskType.PARTNER_CREATE, 5);

        assertThat(task.getState()).isEqualTo(IntegrationTask.TaskState.READY);

        task.markRunning(Instant.now());
        assertThat(task.getState()).isEqualTo(IntegrationTask.TaskState.RUNNING);
        assertThat(task.getAttemptCount()).isEqualTo(1);

        task.markSucceeded(Instant.now());
        assertThat(task.getState()).isEqualTo(IntegrationTask.TaskState.SUCCEEDED);
        assertThat(task.getState().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("RUNNING→RETRY_WAIT on transient failure")
    void task_transientFailure_movesToRetryWait() {
        IntegrationTask task = new IntegrationTask(
                "res-001", null, IntegrationTask.TaskType.PARTNER_CREATE, 5);

        task.markRunning(Instant.now());
        task.markRetryWait("connection timeout", Instant.now().plusSeconds(5), Instant.now());

        assertThat(task.getState()).isEqualTo(IntegrationTask.TaskState.RETRY_WAIT);
        assertThat(task.getLastFailureReason()).contains("connection timeout");
    }

    @Test
    @DisplayName("TERMINAL_FAILED after max attempts exceeded")
    void task_exceedsMaxAttempts_becomesTerminalFailed() {
        IntegrationTask task = new IntegrationTask(
                "res-001", null, IntegrationTask.TaskType.PARTNER_CREATE, 3);

        for (int i = 0; i < 3; i++) {
            task.markRunning(Instant.now());
            task.markTerminalFailed("server error", IntegrationTask.FailureClass.TRANSIENT, Instant.now());
        }

        assertThat(task.getState()).isEqualTo(IntegrationTask.TaskState.TERMINAL_FAILED);
        assertThat(task.getState().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("TERMINAL_FAILED immediately on permanent failure")
    void task_permanentFailure_immediatelyTerminal() {
        IntegrationTask task = new IntegrationTask(
                "res-001", null, IntegrationTask.TaskType.PARTNER_CREATE, 5);

        task.markRunning(Instant.now());
        task.markTerminalFailed("invalid partner id", IntegrationTask.FailureClass.PERMANENT, Instant.now());

        assertThat(task.getState()).isEqualTo(IntegrationTask.TaskState.TERMINAL_FAILED);
        assertThat(task.getFailureClass()).isEqualTo(IntegrationTask.FailureClass.PERMANENT);
    }

    @Test
    @DisplayName("canRetry returns true when below max attempts and not terminal")
    void task_canRetry_belowMaxAttempts() {
        IntegrationTask task = new IntegrationTask(
                "res-001", null, IntegrationTask.TaskType.PARTNER_CANCEL, 5);
        task.markRunning(Instant.now());

        assertThat(task.canRetry()).isTrue();
    }
        @Test
        @DisplayName("isReadyForRetry returns true when retry wait time has elapsed")
        void task_isReadyForRetry_afterWaitTime() {
            IntegrationTask task = new IntegrationTask(
                    "res-001", null, IntegrationTask.TaskType.PARTNER_CANCEL, 5);
            task.markRunning(Instant.now());
            Instant past = Instant.now().minusSeconds(10);
            task.markRetryWait("timeout", past, Instant.now());

            assertThat(task.isReadyForRetry(Instant.now())).isTrue();
        }
}
