package com.acme.reservation.application.command;

import com.acme.reservation.application.ports.inbound.CancelRoomUseCase;
import com.acme.reservation.application.ports.outbound.AuditEventRepository;
import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * US3: Cancel a single room item within a reservation.
 * Idempotency guard prevents duplicate-processing.
 * Atomic: reservation aggregate update + history event within one transaction.
 */
@Service
public class CancelRoomCommandHandler implements CancelRoomUseCase {

    private final ReservationRepository reservationRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditEventRepository auditEventRepository;

    public CancelRoomCommandHandler(
            ReservationRepository reservationRepository,
            IdempotencyRepository idempotencyRepository,
            AuditEventRepository auditEventRepository) {
        this.reservationRepository = reservationRepository;
        this.idempotencyRepository = idempotencyRepository;
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
                String roomStatus = reservation.getRooms().stream()
                        .filter(r -> r.getRoomItemId().equals(command.roomItemId()))
                        .findFirst()
                        .map(r -> r.getStatus().name())
                        .orElse("UNKNOWN");
                return new Result(command.reservationId(), command.roomItemId(), roomStatus, true);
            }
        }

        // Load aggregate
        Reservation reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException(
                        "Reservation not found: " + command.reservationId()));

        // Mark IN_PROGRESS
        Instant now = Instant.now();
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                command.idempotencyKey(),
                IdempotencyRecord.OperationType.CANCEL_ROOM,
                now,
                now.plus(30, ChronoUnit.DAYS));
        idempotencyRepository.save(idempotencyRecord);


        // Apply cancellation to the room item (no fee/refund at this stage — partner will confirm)
        reservation.cancelRoom(
                command.roomItemId(),
                command.reason(),
                BigDecimal.ZERO,   // fee calculated on partner acknowledgement
                BigDecimal.ZERO,   // refund calculated on partner acknowledgement
                now);

        Reservation saved = reservationRepository.save(reservation);

        // Emit audit history event
        ReservationHistoryEvent event = ReservationHistoryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .reservationId(command.reservationId())
                .roomItemId(command.roomItemId())
                .eventType(ReservationHistoryEvent.EventType.ROOM_CANCELLED)
                .actorId(command.actorId())
                .actorType(ReservationHistoryEvent.ActorType.USER)
                .reason(command.reason())
                .traceId(command.traceId())
                .occurredAt(now)
                .build();
        auditEventRepository.save(event);

        // Complete idempotency record
        idempotencyRecord.complete(command.reservationId(), command.reservationId(), now);
        idempotencyRepository.save(idempotencyRecord);

        String roomStatus = saved.getRooms().stream()
                .filter(r -> r.getRoomItemId().equals(command.roomItemId()))
                .findFirst()
                .map(r -> r.getStatus().name())
                .orElse("CANCELLATION_PENDING");

        return new Result(command.reservationId(), command.roomItemId(), roomStatus, false);
    }
}
