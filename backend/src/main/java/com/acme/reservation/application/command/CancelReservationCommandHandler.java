package com.acme.reservation.application.command;

import com.acme.reservation.application.ports.inbound.CancelReservationUseCase;
import com.acme.reservation.application.ports.outbound.AuditEventRepository;
import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.domain.audit.IntegrationTask;
import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * US4: Cancel all rooms in a reservation with idempotency guard.
 * Enqueues an async IntegrationTask for downstream partner notification.
 */
@Service
public class CancelReservationCommandHandler implements CancelReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final IntegrationTaskRepository integrationTaskRepository;
    private final AuditEventRepository auditEventRepository;

    public CancelReservationCommandHandler(
            ReservationRepository reservationRepository,
            IdempotencyRepository idempotencyRepository,
            IntegrationTaskRepository integrationTaskRepository,
            AuditEventRepository auditEventRepository) {
        this.reservationRepository = reservationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.integrationTaskRepository = integrationTaskRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    @Transactional
    public Result execute(Command command) {
        // Idempotency check
        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByKey(command.idempotencyKey());

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.isCompleted()) {
                Reservation reservation = reservationRepository
                        .findById(command.reservationId())
                        .orElseThrow(() -> new javax.persistence.EntityNotFoundException(
                                "Reservation not found: " + command.reservationId()));
                return new Result(command.reservationId(), reservation.getStatus().name(), true);
            }
            if (record.isInProgress()) {
                Reservation reservation = reservationRepository
                        .findById(command.reservationId())
                        .orElseThrow(() -> new javax.persistence.EntityNotFoundException(
                                "Reservation not found: " + command.reservationId()));
                return new Result(command.reservationId(), reservation.getStatus().name(), false);
            }
        }

        // Load aggregate — optimistic locking will throw ObjectOptimisticLockingFailureException
        // on concurrent updates, which the controller maps to HTTP 409
        Reservation reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException(
                        "Reservation not found: " + command.reservationId()));

        Instant now = Instant.now();

        // Mark IN_PROGRESS idempotency record
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                command.idempotencyKey(),
                IdempotencyRecord.OperationType.CANCEL_RESERVATION,
                now,
                now.plus(30, ChronoUnit.DAYS));
                try {
                        idempotencyRepository.save(idempotencyRecord);
                } catch (DataIntegrityViolationException concurrentInsertException) {
                        Optional<IdempotencyRecord> concurrent =
                                        idempotencyRepository.findByKey(command.idempotencyKey());
                        if (concurrent.isPresent()) {
                                Reservation concurrentReservation = reservationRepository
                                                .findById(command.reservationId())
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Reservation not found: " + command.reservationId()));
                                boolean replay = concurrent.get().isCompleted();
                                String status = concurrentReservation.getStatus().name();
                                return new Result(command.reservationId(), status, replay);
                        }
                        throw concurrentInsertException;
                }

        // Cancel all rooms in the aggregate
        reservation.cancelAll(command.reason(), BigDecimal.ZERO, BigDecimal.ZERO, now);
        Reservation saved = reservationRepository.save(reservation);

        // Emit audit history event
        ReservationHistoryEvent event = ReservationHistoryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .reservationId(command.reservationId())
                .eventType(ReservationHistoryEvent.EventType.RESERVATION_CANCELLED)
                .actorId(command.actorId())
                .actorType(ReservationHistoryEvent.ActorType.USER)
                .reason(command.reason())
                .traceId(command.traceId())
                .occurredAt(now)
                .build();
        auditEventRepository.save(event);

        // Enqueue async integration task for partner notification
        IntegrationTask integrationTask = new IntegrationTask(
                command.reservationId(), null,
                IntegrationTask.TaskType.PARTNER_CANCEL, 5);
        integrationTaskRepository.save(integrationTask);

        // Complete idempotency record
        idempotencyRecord.complete(command.reservationId(), command.reservationId(), now);
        idempotencyRepository.save(idempotencyRecord);

        return new Result(command.reservationId(), saved.getStatus().name(), false);
    }
}
