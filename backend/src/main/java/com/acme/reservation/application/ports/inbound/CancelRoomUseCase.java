package com.acme.reservation.application.ports.inbound;

/**
 * Inbound port: cancel a single room within a reservation.
 */
public interface CancelRoomUseCase {

        final class Command {
                private final String reservationId;
                private final String roomItemId;
                private final String idempotencyKey;
                private final String reason;
                private final String actorId;
                private final String traceId;

                public Command(
                                String reservationId,
                                String roomItemId,
                                String idempotencyKey,
                                String reason,
                                String actorId,
                                String traceId) {
                        this.reservationId = reservationId;
                        this.roomItemId = roomItemId;
                        this.idempotencyKey = idempotencyKey;
                        this.reason = reason;
                        this.actorId = actorId;
                        this.traceId = traceId;
                }

                public String reservationId() {
                        return reservationId;
                }

                public String roomItemId() {
                        return roomItemId;
                }

                public String idempotencyKey() {
                        return idempotencyKey;
                }

                public String reason() {
                        return reason;
                }

                public String actorId() {
                        return actorId;
                }

                public String traceId() {
                        return traceId;
                }
        }

        final class Result {
                private final String reservationId;
                private final String roomItemId;
                private final String status;
                private final boolean idempotentReplay;

                public Result(String reservationId, String roomItemId, String status, boolean idempotentReplay) {
                        this.reservationId = reservationId;
                        this.roomItemId = roomItemId;
                        this.status = status;
                        this.idempotentReplay = idempotentReplay;
                }

                public String reservationId() {
                        return reservationId;
                }

                public String roomItemId() {
                        return roomItemId;
                }

                public String status() {
                        return status;
                }

                public boolean idempotentReplay() {
                        return idempotentReplay;
                }
        }

    Result execute(Command command);
}
