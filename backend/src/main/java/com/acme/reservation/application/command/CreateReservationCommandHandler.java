package com.acme.reservation.application.command;

import com.acme.reservation.application.ports.inbound.CreateReservationUseCase;
import com.acme.reservation.application.ports.outbound.AuditEventRepository;
import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.configuration.MetricsConfig.ReservationMetrics;
import com.acme.reservation.domain.audit.IntegrationTask;
import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import com.acme.reservation.domain.reservation.ReservationRequestContext;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * US1: Create a multi-room reservation with idempotency guard.
 * Transactional upsert via idempotency_records ensures exactly-once processing.
 * Returns contextId for polling async request state via GET /api/v1/reservations/{contextId}/status
 */
@Service
public class CreateReservationCommandHandler implements CreateReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final RequestContextRepository requestContextRepository;
    private final IntegrationTaskRepository integrationTaskRepository;
    private final AuditEventRepository auditEventRepository;
        private final ReservationMetrics reservationMetrics;

    public CreateReservationCommandHandler(
            ReservationRepository reservationRepository,
            IdempotencyRepository idempotencyRepository,
            RequestContextRepository requestContextRepository,
            IntegrationTaskRepository integrationTaskRepository,
                        AuditEventRepository auditEventRepository,
                        ReservationMetrics reservationMetrics) {
        this.reservationRepository = reservationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.requestContextRepository = requestContextRepository;
        this.integrationTaskRepository = integrationTaskRepository;
        this.auditEventRepository = auditEventRepository;
                this.reservationMetrics = reservationMetrics;
    }

    @Override
    @Transactional
    public Result execute(Command command) {
        Instant now = Instant.now();
        reservationMetrics.incrementCreateRequests();
        
        try {
            // Compute fingerprint for conflict detection
            String fingerprint = computeFingerprint(command);
            
            // Idempotency guard with fingerprint matching
            Optional<IdempotencyRecord> existing =
                    idempotencyRepository.findByKey(command.idempotencyKey());

            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();

                if (record.getOperationType() != IdempotencyRecord.OperationType.CREATE) {
                    throw new IllegalArgumentException(
                            "Idempotency key reused with conflicting business context (409)");
                }
                
                // 409 Conflict: Key reused with different business context
                if (record.getFingerprint() != null
                        && !record.matchesFingerprint(fingerprint)) {
                    throw new IllegalArgumentException(
                            "Idempotency key reused with conflicting business context (409)");
                }
                
                // 200 OK: Idempotent replay — already succeeded
                if (record.isCompleted() && record.getReservationId() != null) {
                    reservationMetrics.incrementIdempotencyHit();
                    String reservationId = record.getReservationId();
                    String status = reservationRepository.findById(reservationId)
                            .map(r -> r.getStatus().name())
                            .orElse("PENDING");
                    return new Result(
                            reservationId,
                            status,
                            "COMPLETED",
                            true);
                }

                // 202 Accepted: Concurrent duplicate — in progress
                if (record.isInProgress()) {
                    reservationMetrics.incrementIdempotencyHit();
                    return new Result(
                            record.getReservationId() != null ? record.getReservationId() : "pending",
                            "PENDING",
                            "IN_PROGRESS",
                            false);
                }
            }

            // Create IN_PROGRESS idempotency record with fingerprint
            IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                    command.idempotencyKey(),
                    fingerprint,
                    30);
            try {
                idempotencyRepository.save(idempotencyRecord);
            } catch (DataIntegrityViolationException concurrentInsertException) {
                Optional<IdempotencyRecord> concurrent =
                        idempotencyRepository.findByKey(command.idempotencyKey());
                if (concurrent.isPresent()) {
                    IdempotencyRecord concurrentRecord = concurrent.get();
                    if (concurrentRecord.getOperationType()
                            != IdempotencyRecord.OperationType.CREATE) {
                        throw new IllegalArgumentException(
                                "Idempotency key reused with conflicting business context (409)");
                    }
                    if (concurrentRecord.getFingerprint() != null
                            && !concurrentRecord.matchesFingerprint(fingerprint)) {
                        throw new IllegalArgumentException(
                                "Idempotency key reused with conflicting business context (409)");
                    }
                    reservationMetrics.incrementIdempotencyHit();
                    if (concurrentRecord.getReservationId() != null) {
                        String reservationId = concurrentRecord.getReservationId();
                        String status = reservationRepository.findById(reservationId)
                                .map(r -> r.getStatus().name())
                                .orElse("PENDING");
                        String processingStatus = concurrentRecord.isCompleted()
                                ? "COMPLETED"
                                : "IN_PROGRESS";
                        return new Result(
                                reservationId,
                                status,
                                processingStatus,
                                concurrentRecord.isCompleted());
                    }
                    return new Result("pending", "PENDING", "IN_PROGRESS", false);
                }
                throw concurrentInsertException;
            }

            // Create ReservationRequestContext (RECEIVED status)
            ReservationRequestContext requestContext = ReservationRequestContext.create(
                    command.idempotencyKey(),
                    command.traceId());

            // Transition: RECEIVED → VALIDATED
            requestContext.markValidated();

            // Persist request context after the referenced idempotency record exists
            ReservationRequestContext savedContext = requestContextRepository.save(requestContext);

            // Build room items — guestId is the tokenized PII reference
            String guestId = "guest:" + command.guest().givenName() + ":"
                    + command.guest().familyName();

            List<RoomReservationItem> rooms = command.rooms().stream()
                    .map(r -> RoomReservationItem.builder()
                            .roomCode(r.roomCode())
                            .checkInDate(r.checkInDate())
                            .checkOutDate(r.checkOutDate())
                            .basePrice(r.basePrice())
                            .reservationId("placeholder")
                            .build())
                    .collect(Collectors.toList());

            // Create Reservation aggregate
            Reservation reservation = Reservation.create(
                    command.partnerId(),
                    guestId,
                    command.currencyCode(),
                    command.externalReference(),
                    rooms);

            // Fix reservationId references in rooms
            List<RoomReservationItem> fixedRooms = command.rooms().stream()
                    .map(r -> RoomReservationItem.builder()
                            .roomCode(r.roomCode())
                            .reservationId(reservation.getReservationId())
                            .checkInDate(r.checkInDate())
                            .checkOutDate(r.checkOutDate())
                            .basePrice(r.basePrice())
                            .build())
                    .collect(Collectors.toList());

            Reservation finalReservation = Reservation.create(
                    command.partnerId(),
                    guestId,
                    command.currencyCode(),
                    command.externalReference(),
                    fixedRooms);

            Reservation saved = reservationRepository.save(finalReservation);

            // Transition: VALIDATED → QUEUED (request enqueued for async processing)
            savedContext.markQueued();
            requestContextRepository.updateStatus(savedContext.getContextId(), ReservationRequestContext.Status.QUEUED);

            // Enqueue integration task for partner interaction
            IntegrationTask task = new IntegrationTask(
                    saved.getReservationId(),
                    savedContext.getContextId(),
                    IntegrationTask.TaskType.PARTNER_CREATE,
                    5);
            integrationTaskRepository.save(task);
            reservationMetrics.updateQueueDepth(1);

            // Complete idempotency record (link to request context, not just reservation)
            idempotencyRecord.complete(saved.getReservationId(), savedContext.getContextId(), now);
            idempotencyRepository.save(idempotencyRecord);

            // Emit audit event
            ReservationHistoryEvent historyEvent = ReservationHistoryEvent.builder()
                    .reservationId(saved.getReservationId())
                    .eventType(ReservationHistoryEvent.EventType.CREATED)
                    .actorId(command.actorId())
                    .actorType(ReservationHistoryEvent.ActorType.USER)
                    .traceId(command.traceId())
                    .occurredAt(now)
                    .build();
            auditEventRepository.save(historyEvent);

            // Return 202 Accepted with contextId for polling
            return new Result(
                    saved.getReservationId(),
                    savedContext.getStatus().name(),
                    "QUEUED",
                    false);
        } catch (RuntimeException runtimeException) {
            reservationMetrics.incrementCreateErrors();
            throw runtimeException;
        } finally {
            reservationMetrics.recordCreateLatency(java.time.Duration.between(now, Instant.now()));
        }
    }

    /**
     * Compute deterministic fingerprint of business context to detect key reuse.
     * Fingerprint = hash of (guest givenName, guest familyName, partnerId, checkInDateRange)
     */
    private String computeFingerprint(Command command) {
        return Integer.toHexString(
                (command.guest().givenName() +
                 command.guest().familyName() +
                 command.partnerId() +
                 command.rooms().stream()
                         .map(r -> r.checkInDate() + ":" + r.checkOutDate())
                         .collect(Collectors.joining("|"))).hashCode());
    }
}
