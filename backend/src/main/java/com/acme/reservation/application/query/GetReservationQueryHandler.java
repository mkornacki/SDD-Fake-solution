package com.acme.reservation.application.query;

import com.acme.reservation.application.ports.inbound.GetReservationUseCase;
import com.acme.reservation.application.ports.outbound.AuditEventRepository;
import com.acme.reservation.application.ports.outbound.FinancialBreakdownRepository;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.domain.financial.FinancialBreakdown;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * US2: Retrieve reservation state, room items, history, and optional financial breakdown.
 * PII masking is applied at the HTTP adapter layer (ReservationResponseMapper),
 * not here — this handler returns the full domain objects.
 */
@Service
public class GetReservationQueryHandler implements GetReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final AuditEventRepository auditEventRepository;
    private final FinancialBreakdownRepository financialBreakdownRepository;

    public GetReservationQueryHandler(
            ReservationRepository reservationRepository,
            AuditEventRepository auditEventRepository,
            FinancialBreakdownRepository financialBreakdownRepository) {
        this.reservationRepository = reservationRepository;
        this.auditEventRepository = auditEventRepository;
        this.financialBreakdownRepository = financialBreakdownRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Result execute(Query query) {
        Reservation reservation = reservationRepository.findById(query.reservationId())
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException(
                        "Reservation not found: " + query.reservationId()));

        List<ReservationHistoryEvent> history =
                auditEventRepository.findByReservationIdOrderedByOccurredAt(query.reservationId());

        FinancialBreakdown financialBreakdown =
                financialBreakdownRepository.findByReservationId(query.reservationId())
                        .orElse(null);

        return new Result(reservation, history, financialBreakdown);
    }
}
