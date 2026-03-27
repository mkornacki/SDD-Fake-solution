package com.acme.reservation.domain;

import com.acme.reservation.domain.reservation.ReservationRequestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReservationRequestContext state machine transitions.
 * Tests the valid sequence: RECEIVED → VALIDATED → QUEUED → PROCESSING → COMPLETED (or FAILED/DLQ)
 */
class RequestContextStateTest {

  @Test
  void testInitialStateIsReceived() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");

    assertEquals(ReservationRequestContext.Status.RECEIVED, context.getStatus());
    assertNotNull(context.getContextId());
    assertEquals("idempotency-key-123", context.getIdempotencyKey());
    assertEquals("correlation-id-456", context.getCorrelationId());
  }

  @Test
  void testValidTransitionReceivedToValidated() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");

    context.markValidated();

    assertEquals(ReservationRequestContext.Status.VALIDATED, context.getStatus());
  }

  @Test
  void testValidTransitionValidatedToQueued() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();

    context.markQueued();

    assertEquals(ReservationRequestContext.Status.QUEUED, context.getStatus());
  }

  @Test
  void testValidTransitionQueuedToProcessing() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();
    context.markQueued();

    context.markProcessing();

    assertEquals(ReservationRequestContext.Status.PROCESSING, context.getStatus());
  }

  @Test
  void testValidTransitionProcessingToCompleted() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();
    context.markQueued();
    context.markProcessing();

    context.markCompleted();

    assertEquals(ReservationRequestContext.Status.COMPLETED, context.getStatus());
    assertTrue(context.isTerminal());
  }

  @Test
  void testValidTransitionProcessingToFailed() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();
    context.markQueued();
    context.markProcessing();

    context.markFailed("Connection timeout");

    assertEquals(ReservationRequestContext.Status.FAILED, context.getStatus());
    assertEquals("Connection timeout", context.getErrorContext());
    assertTrue(context.isTerminal());
  }

  @Test
  void testValidTransitionProcessingToDlq() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();
    context.markQueued();
    context.markProcessing();

    context.markDlq("Max retries exhausted: permanent failure detected");

    assertEquals(ReservationRequestContext.Status.DLQ, context.getStatus());
    assertEquals("Max retries exhausted: permanent failure detected", context.getErrorContext());
    assertTrue(context.isTerminal());
  }

  @Test
  void testInvalidTransitionReceivedToQueued() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");

    assertThrows(IllegalStateException.class, () -> context.markQueued());
  }

  @Test
  void testInvalidTransitionReceivedToProcessing() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");

    assertThrows(IllegalStateException.class, () -> context.markProcessing());
  }

  @Test
  void testInvalidTransitionValidatedToProcessing() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();

    assertThrows(IllegalStateException.class, () -> context.markProcessing());
  }

  @Test
  void testInvalidTransitionQueuedToValidated() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();
    context.markQueued();

    assertThrows(IllegalStateException.class, () -> context.markValidated());
  }

  @Test
  void testTerminalStateCannotTransition() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");
    context.markValidated();
    context.markQueued();
    context.markProcessing();
    context.markCompleted();

    // Try to transition from terminal state
    assertThrows(IllegalStateException.class, () -> context.markFailed("Error"));
    assertThrows(IllegalStateException.class, () -> context.markProcessing());
    assertThrows(IllegalStateException.class, () -> context.markDlq("Error"));
  }

  @Test
  void testTimestampsAreUpdated() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-123", "correlation-id-456");

    long createdTime = context.getCreatedAt().toEpochMilli();
    long updatedTime1 = context.getUpdatedAt().toEpochMilli();

    // Wait a tiny bit to ensure different timestamp
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      // ignore
    }

    context.markValidated();
    long updatedTime2 = context.getUpdatedAt().toEpochMilli();

    assertTrue(createdTime <= updatedTime1);
    assertTrue(updatedTime1 <= updatedTime2);
  }

  @Test
  void testIsTerminalFlags() {
    ReservationRequestContext context1 = ReservationRequestContext.create(
        "key-1", "corr-1");
    assertFalse(context1.isTerminal());

    context1.markValidated();
    assertFalse(context1.isTerminal());

    context1.markQueued();
    assertFalse(context1.isTerminal());

    context1.markProcessing();
    assertFalse(context1.isTerminal());

    context1.markCompleted();
    assertTrue(context1.isTerminal());

    // Test FAILED terminal state
    ReservationRequestContext context2 = ReservationRequestContext.create("key-2", "corr-2");
    context2.markValidated();
    context2.markQueued();
    context2.markProcessing();
    context2.markFailed("Error");
    assertTrue(context2.isTerminal());

    // Test DLQ terminal state
    ReservationRequestContext context3 = ReservationRequestContext.create("key-3", "corr-3");
    context3.markValidated();
    context3.markQueued();
    context3.markProcessing();
    context3.markDlq("Max retries");
    assertTrue(context3.isTerminal());
  }

  @Test
  void testFullHappyPathFlow() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-full", "correlation-full");

    assertEquals(ReservationRequestContext.Status.RECEIVED, context.getStatus());

    context.markValidated();
    assertEquals(ReservationRequestContext.Status.VALIDATED, context.getStatus());

    context.markQueued();
    assertEquals(ReservationRequestContext.Status.QUEUED, context.getStatus());

    context.markProcessing();
    assertEquals(ReservationRequestContext.Status.PROCESSING, context.getStatus());

    context.markCompleted();
    assertEquals(ReservationRequestContext.Status.COMPLETED, context.getStatus());
    assertTrue(context.isTerminal());
  }

  @Test
  void testErrorFlowWithFailure() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-error", "correlation-error");

    context.markValidated();
    context.markQueued();
    context.markProcessing();
    context.markFailed("Partner returned 503 Service Unavailable");

    assertEquals(ReservationRequestContext.Status.FAILED, context.getStatus());
    assertEquals("Partner returned 503 Service Unavailable", context.getErrorContext());
    assertTrue(context.isTerminal());
  }

  @Test
  void testErrorFlowWithDLQRouting() {
    ReservationRequestContext context = ReservationRequestContext.create(
        "idempotency-key-dlq", "correlation-dlq");

    context.markValidated();
    context.markQueued();
    context.markProcessing();
    context.markDlq("Permanent failure: duplicate reservation already exists");

    assertEquals(ReservationRequestContext.Status.DLQ, context.getStatus());
    assertTrue(context.isTerminal());
  }
}
