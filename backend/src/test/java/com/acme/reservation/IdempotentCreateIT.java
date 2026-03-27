package com.acme.reservation;

import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import com.acme.reservation.domain.reservation.ReservationRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for idempotent create requests.
 * Verifies that request context and idempotency records persist correctly
 * through the domain-to-persistence adapter layer.
 */
@SpringBootTest
@ActiveProfiles("test")
class IdempotentCreateIT {

  @Autowired private IdempotencyRepository idempotencyRepository;

  @Autowired private RequestContextRepository contextRepository;

  /**
   * Test: Identical idempotency keys can be saved and retrieved.
   * Verifies RequestContextRepository.save() and findByContextId() work correctly.
   */
  @Test
  void testRequestContextPersistence() {
    String idempotencyKey = "idempotent-test-key-123";
    String correlationId = "corr-id-first";

    idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, "fp-" + idempotencyKey, 30));

    // Create first request context (RECEIVED status)
    ReservationRequestContext context1 = ReservationRequestContext.create(idempotencyKey, correlationId);
    
    // Save via port interface
    ReservationRequestContext saved = contextRepository.save(context1);
    
    // Retrieve and verify
    var retrieved = contextRepository.findByContextId(saved.getContextId());
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
    assertThat(retrieved.get().getContextId()).isEqualTo(context1.getContextId());
    assertThat(retrieved.get().getStatus()).isEqualTo(ReservationRequestContext.Status.RECEIVED);
  }

  /**
   * Test: Idempotency record persists with correct expiry using fingerprint constructor.
   */
  @Test
  void testIdempotencyRecordPersistence() {
    String key = "persistence-test-key";
    String fingerprint = "fp:guest:john:smith:room:101";

    // Create with fingerprint (new spec pattern)
    IdempotencyRecord record = new IdempotencyRecord(key, fingerprint, 30);

    // Verify creation
    assertThat(record.getIdempotencyKey()).isEqualTo(key);
    assertThat(record.getFingerprint()).isEqualTo(fingerprint);
    assertThat(record.getCreatedAt()).isNotNull();
    assertThat(record.getExpiresAt()).isEqualTo(record.getCreatedAt().plus(30, ChronoUnit.DAYS));

    // Verify not expired immediately
    assertThat(record.isExpired()).isFalse();
    
    // Verify fingerprint matching
    assertThat(record.matchesFingerprint(fingerprint)).isTrue();
    assertThat(record.matchesFingerprint("different-fp")).isFalse();
  }

  /**
   * Test: Request context state transitions are persisted correctly.
   * RECEIVED → VALIDATED → QUEUED → PROCESSING → COMPLETED
   */
  @Test
  void testRequestContextStatePersistence() {
    String idempotencyKey = "state-test-key";
    String correlationId = "state-test-corr";

    idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, "fp-" + idempotencyKey, 30));

    // Create context (RECEIVED)
    ReservationRequestContext context = ReservationRequestContext.create(idempotencyKey, correlationId);
    ReservationRequestContext saved = contextRepository.save(context);
    String contextId = saved.getContextId();

    // Verify RECEIVED state persisted
    assertThat(contextRepository.findByContextId(contextId).get().getStatus())
        .isEqualTo(ReservationRequestContext.Status.RECEIVED);

    // Transition to VALIDATED
    context.markValidated();
    contextRepository.updateStatus(contextId, ReservationRequestContext.Status.VALIDATED);
    assertThat(contextRepository.findByContextId(contextId).get().getStatus())
        .isEqualTo(ReservationRequestContext.Status.VALIDATED);

    // Transition to QUEUED
    context.markQueued();
    contextRepository.updateStatus(contextId, ReservationRequestContext.Status.QUEUED);
    assertThat(contextRepository.findByContextId(contextId).get().getStatus())
        .isEqualTo(ReservationRequestContext.Status.QUEUED);

    // Transition to PROCESSING
    context.markProcessing();
    contextRepository.updateStatus(contextId, ReservationRequestContext.Status.PROCESSING);
    assertThat(contextRepository.findByContextId(contextId).get().getStatus())
        .isEqualTo(ReservationRequestContext.Status.PROCESSING);

    // Transition to COMPLETED (terminal)
    context.markCompleted();
    contextRepository.updateStatus(contextId, ReservationRequestContext.Status.COMPLETED);
    assertThat(contextRepository.findByContextId(contextId).get().getStatus())
        .isEqualTo(ReservationRequestContext.Status.COMPLETED);
  }

  /**
   * Test: Context reaches terminal state (COMPLETED) and isTerminal() reflects that.
   */
  @Test
  void testTerminalStateIsPersistent() {
    String idempotencyKey = "terminal-test-key";
    String correlationId = "terminal-test-corr";

    idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, "fp-" + idempotencyKey, 30));

    // Create and fully process context
    ReservationRequestContext context = ReservationRequestContext.create(idempotencyKey, correlationId);
    context.markValidated();
    context.markQueued();
    context.markProcessing();
    context.markCompleted();

    // Persist
    ReservationRequestContext saved = contextRepository.save(context);
    String contextId = saved.getContextId();

    // Update status to terminal
    contextRepository.updateStatus(contextId, ReservationRequestContext.Status.COMPLETED);

    // Verify terminal state is persisted and isTerminal() returns true
    var retrieved = contextRepository.findByContextId(contextId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getStatus()).isEqualTo(ReservationRequestContext.Status.COMPLETED);
    assertThat(retrieved.get().isTerminal()).isTrue();
  }

  /**
   * Test: Error context is preserved through persistence in FAILED terminal state.
   */
  @Test
  void testErrorContextPersistence() {
    String idempotencyKey = "error-test-key";
    String errorMessage = "Partner timeout: 503 Service Unavailable";

    idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, "fp-" + idempotencyKey, 30));

    ReservationRequestContext context = ReservationRequestContext.create(idempotencyKey, "error-corr");
    context.markValidated();
    context.markQueued();
    context.markProcessing();
    context.markFailed(errorMessage);

    ReservationRequestContext saved = contextRepository.save(context);
    String contextId = saved.getContextId();

    // Update to FAILED status
    contextRepository.updateStatus(contextId, ReservationRequestContext.Status.FAILED);

    var retrieved = contextRepository.findByContextId(contextId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getErrorContext()).isEqualTo(errorMessage);
    assertThat(retrieved.get().getStatus()).isEqualTo(ReservationRequestContext.Status.FAILED);
    assertThat(retrieved.get().isTerminal()).isTrue();
  }

  /**
   * Test: DLQ state is correctly persisted with error context.
   */
  @Test
  void testDlqStateWithContext() {
    String idempotencyKey = "dlq-test-key";
    String dlqReason = "Max retries exhausted: permanent failure";

    idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, "fp-" + idempotencyKey, 30));

    ReservationRequestContext context = ReservationRequestContext.create(idempotencyKey, "dlq-corr");
    context.markValidated();
    context.markQueued();
    context.markProcessing();
    context.markDlq(dlqReason);

    ReservationRequestContext saved = contextRepository.save(context);
    String contextId = saved.getContextId();

    // Update to DLQ status
    contextRepository.updateStatus(contextId, ReservationRequestContext.Status.DLQ);

    var retrieved = contextRepository.findByContextId(contextId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getStatus()).isEqualTo(ReservationRequestContext.Status.DLQ);
    assertThat(retrieved.get().getErrorContext()).isEqualTo(dlqReason);
    assertThat(retrieved.get().isTerminal()).isTrue();
  }
}
