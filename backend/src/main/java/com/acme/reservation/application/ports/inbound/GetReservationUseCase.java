package com.acme.reservation.application.ports.inbound;

import com.acme.reservation.domain.financial.FinancialBreakdown;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;

import java.util.List;

/**
 * Inbound port: retrieve reservation state and history.
 */
public interface GetReservationUseCase {

        final class Query {
                private final String reservationId;
                private final String actorId;
                private final boolean hasPiiAccess;

                public Query(String reservationId, String actorId, boolean hasPiiAccess) {
                        this.reservationId = reservationId;
                        this.actorId = actorId;
                        this.hasPiiAccess = hasPiiAccess;
                }

                public String reservationId() {
                        return reservationId;
                }

                public String actorId() {
                        return actorId;
                }

                public boolean hasPiiAccess() {
                        return hasPiiAccess;
                }
        }

        final class Result {
                private final Reservation reservation;
                private final List<ReservationHistoryEvent> history;
                private final FinancialBreakdown financialBreakdown;

                public Result(
                                Reservation reservation,
                                List<ReservationHistoryEvent> history,
                                FinancialBreakdown financialBreakdown) {
                        this.reservation = reservation;
                        this.history = history;
                        this.financialBreakdown = financialBreakdown;
                }

                public Reservation reservation() {
                        return reservation;
                }

                public List<ReservationHistoryEvent> history() {
                        return history;
                }

                public FinancialBreakdown financialBreakdown() {
                        return financialBreakdown;
                }
        }

    Result execute(Query query);
}
