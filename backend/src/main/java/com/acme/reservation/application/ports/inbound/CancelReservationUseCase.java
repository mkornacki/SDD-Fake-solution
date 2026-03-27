package com.acme.reservation.application.ports.inbound;

/**
 * Inbound port: cancel all rooms in a reservation.
 */
public interface CancelReservationUseCase {

        final class Command {
                private final String reservationId;
                private final String idempotencyKey;
                private final String reason;
                private final String actorId;
                private final String traceId;

                public Command(
                                String reservationId,
                                String idempotencyKey,
                                String reason,
                                String actorId,
                                String traceId) {
                        this.reservationId = reservationId;
                        this.idempotencyKey = idempotencyKey;
                        this.reason = reason;
                        this.actorId = actorId;
                        this.traceId = traceId;
                }

                public String reservationId() {
                        return reservationId;
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
                private final String status;
                private final boolean idempotentReplay;

                public Result(String reservationId, String status, boolean idempotentReplay) {
                        this.reservationId = reservationId;
                        this.status = status;
                        this.idempotentReplay = idempotentReplay;
                }

                public String reservationId() {
                        return reservationId;
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
