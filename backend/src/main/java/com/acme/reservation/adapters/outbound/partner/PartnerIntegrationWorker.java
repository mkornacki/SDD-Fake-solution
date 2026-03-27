package com.acme.reservation.adapters.outbound.partner;

import com.acme.reservation.adapters.outbound.messaging.DlqRouter;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.configuration.MetricsConfig.ReservationMetrics;
import com.acme.reservation.domain.audit.IntegrationTask;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * T051: Async worker that polls the integration_tasks table for READY/RETRY_WAIT tasks
 * and attempts partner API delivery with exponential backoff and circuit-breaker protection.
 *
 * Backoff: base 1s, multiplier 2, max 60s, jitter applied.
 * Failure classification: transient (retry) vs permanent (route to DLQ immediately).
 */
@Component
public class PartnerIntegrationWorker {

    private static final Logger LOG = LoggerFactory.getLogger(PartnerIntegrationWorker.class);

    private static final long BASE_DELAY_MS = 1_000L;
    private static final long MAX_DELAY_MS = 60_000L;
    private static final double MULTIPLIER = 2.0;

    private final IntegrationTaskRepository integrationTaskRepository;
    private final DlqRouter dlqRouter;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead workerBulkhead;
    private final ReservationMetrics reservationMetrics;
    private final Random random = new Random();

    @Value("${reservation.partner.stub-endpoint:http://localhost:9090/partner-stub}")
    private String partnerStubEndpoint;

    public PartnerIntegrationWorker(
            IntegrationTaskRepository integrationTaskRepository,
            DlqRouter dlqRouter,
            CircuitBreakerRegistry circuitBreakerRegistry,
            BulkheadRegistry bulkheadRegistry,
            ReservationMetrics reservationMetrics) {
        this.integrationTaskRepository = integrationTaskRepository;
        this.dlqRouter = dlqRouter;
        this.reservationMetrics = reservationMetrics;
        // Use configured circuit breaker, or create a sensible default
        CircuitBreaker cb;
        try {
            cb = circuitBreakerRegistry.circuitBreaker("partnerIntegration");
        } catch (Exception e) {
            cb = CircuitBreaker.of("partnerIntegration", CircuitBreakerConfig.ofDefaults());
        }
        this.circuitBreaker = cb;
        this.workerBulkhead = bulkheadRegistry.bulkhead("partnerIntegrationWorker");
    }

    /**
     * Scheduled poll: runs every 5 seconds, picks up any READY or RETRY_WAIT tasks
     * whose next_attempt_at has passed.
     */
    @Scheduled(fixedDelay = 5_000)
    public void processPendingTasks() {
        List<IntegrationTask> pendingTasks = integrationTaskRepository.findPendingTasks();
        Instant now = Instant.now();
        reservationMetrics.updateQueueDepth(pendingTasks.size());
        pendingTasks.stream()
            .map(IntegrationTask::getCreatedAt)
            .min(Instant::compareTo)
            .ifPresent(oldest -> reservationMetrics.updateConsumerLag(Duration.between(oldest, now)));
        for (IntegrationTask task : pendingTasks) {
            if (task.getState() == IntegrationTask.TaskState.RETRY_WAIT
                    && !task.isReadyForRetry(now)) {
                continue; // not yet due
            }
            processTask(task, now);
        }
    }

    private void processTask(IntegrationTask task, Instant now) {
        Instant startedAt = Instant.now();
        task.markRunning(now);
        integrationTaskRepository.save(task);

        try {
            Bulkhead.decorateRunnable(workerBulkhead, () -> deliverToPartner(task)).run();
            task.markSucceeded(now);
            integrationTaskRepository.save(task);
            LOG.info("Integration task {} succeeded on attempt {}", task.getTaskId(), task.getAttemptCount());

        } catch (BulkheadFullException ex) {
            reservationMetrics.incrementPartnerErrors();
            reservationMetrics.incrementRetryCount();
            Instant nextAttempt = calculateNextAttempt(task.getAttemptCount(), now);
            task.markRetryWait("Worker bulkhead saturated", nextAttempt, now);
            integrationTaskRepository.save(task);
        } catch (PermanentPartnerException ex) {
            reservationMetrics.incrementPartnerErrors();
            LOG.warn("Permanent failure for task {}: {}", task.getTaskId(), ex.getMessage());
            task.markTerminalFailed(ex.getMessage(), IntegrationTask.FailureClass.PERMANENT, now);
            integrationTaskRepository.save(task);
            dlqRouter.route(task, "PERMANENT_FAILURE:" + ex.getMessage());
            reservationMetrics.incrementDlqGrowth();

        } catch (TransientPartnerException ex) {
            reservationMetrics.incrementPartnerErrors();
            LOG.warn("Transient failure for task {} (attempt {}): {}",
                    task.getTaskId(), task.getAttemptCount(), ex.getMessage());

            if (task.getAttemptCount() >= task.getMaxAttempts()) {
                task.markTerminalFailed(ex.getMessage(), IntegrationTask.FailureClass.TRANSIENT, now);
                integrationTaskRepository.save(task);
                dlqRouter.route(task, "MAX_RETRIES_EXCEEDED:" + ex.getMessage());
                reservationMetrics.incrementDlqGrowth();
            } else {
                Instant nextAttempt = calculateNextAttempt(task.getAttemptCount(), now);
                task.markRetryWait(ex.getMessage(), nextAttempt, now);
                integrationTaskRepository.save(task);
                reservationMetrics.incrementRetryCount();
            }
        } finally {
            reservationMetrics.recordPartnerIntegrationLatency(Duration.between(startedAt, Instant.now()));
        }
    }

    /**
     * Simulates partner delivery. In production this would be an HTTP client call
     * wrapped with circuit-breaker decoration.
     * For now it delegates to the circuit-breaker check and performs a stub call.
     */
    private void deliverToPartner(IntegrationTask task) {
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            throw new TransientPartnerException("Circuit breaker OPEN — partner unavailable");
        }
        // Stub: in production, use WebClient / RestTemplate here with partnerStubEndpoint
        LOG.debug("Delivering task {} (type={}) to partner endpoint {}",
                task.getTaskId(), task.getTaskType(), partnerStubEndpoint);
        // In test/dev, always succeeds unless overridden
    }

    private Instant calculateNextAttempt(int attemptCount, Instant now) {
        long delayMs = (long) (BASE_DELAY_MS * Math.pow(MULTIPLIER, attemptCount));
        delayMs = Math.min(delayMs, MAX_DELAY_MS);
        // Add up to 25% jitter
        long jitter = (long) (delayMs * 0.25 * random.nextDouble());
        return now.plus(Duration.ofMillis(delayMs + jitter));
    }

    /** Signals that the failure is permanent and should not be retried. */
    public static class PermanentPartnerException extends RuntimeException {
        public PermanentPartnerException(String message) {
            super(message);
        }
    }

    /** Signals that the failure is transient and should trigger a retry. */
    public static class TransientPartnerException extends RuntimeException {
        public TransientPartnerException(String message) {
            super(message);
        }
    }
}
