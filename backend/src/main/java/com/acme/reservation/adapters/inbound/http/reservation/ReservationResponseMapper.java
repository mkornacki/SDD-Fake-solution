package com.acme.reservation.adapters.inbound.http.reservation;

import com.acme.reservation.application.ports.inbound.GetReservationUseCase;
import com.acme.reservation.domain.financial.FinancialBreakdown;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps domain objects to HTTP response DTOs, applying PII masking based on caller scope.
 * All guest PII fields (givenName, familyName, email, phone) are masked for callers
 * without pii:read scope.
 */
@Component
public class ReservationResponseMapper {

    private static final String MASKED = "***";

    /** Maps a full GetReservationUseCase.Result to a response map (matching ReservationResponse schema). */
    public Map<String, Object> toResponse(GetReservationUseCase.Result result, boolean hasPiiAccess) {
        Reservation r = result.reservation();

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("reservationId", r.getReservationId());
        response.put("partnerId", r.getPartnerId());
        response.put("externalReference", r.getExternalReference());
        response.put("status", r.getStatus().name());
        response.put("currencyCode", r.getCurrencyCode());
        response.put("totalPrice", r.getTotalPrice());
        response.put("totalRefundAmount", r.getTotalRefundAmount());
        response.put("totalCancellationFee", r.getTotalCancellationFee());
        response.put("guest", buildGuestInfo(r.getGuestId(), hasPiiAccess));
        response.put("rooms", r.getRooms().stream().map(this::buildRoomItem).collect(Collectors.toList()));
        response.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        response.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);

        if (result.financialBreakdown() != null) {
            response.put("financialBreakdown", buildFinancialBreakdown(result.financialBreakdown()));
        }

        if (result.history() != null && !result.history().isEmpty()) {
            response.put("history", result.history().stream()
                    .map(this::buildHistoryEvent).collect(Collectors.toList()));
        }

        return response;
    }

    /**
     * Builds the guest info section from the encoded guestId string.
     * The guestId is stored as "guest:GivenName:FamilyName" by CreateReservationCommandHandler.
     * When hasPiiAccess is false, all fields are replaced with "***".
     */
    private Map<String, Object> buildGuestInfo(String guestId, boolean hasPiiAccess) {
        Map<String, Object> guest = new java.util.LinkedHashMap<>();

        if (!hasPiiAccess) {
            guest.put("givenName", MASKED);
            guest.put("familyName", MASKED);
            guest.put("email", MASKED);
            guest.put("phone", MASKED);
            return guest;
        }

        // Parse encoded guestId ("guest:GivenName:FamilyName")
        if (guestId != null && guestId.startsWith("guest:")) {
            String[] parts = guestId.split(":", 3);
            guest.put("givenName", parts.length > 1 ? parts[1] : null);
            guest.put("familyName", parts.length > 2 ? parts[2] : null);
        } else {
            guest.put("givenName", guestId);
            guest.put("familyName", null);
        }
        guest.put("email", null);  // Not currently stored in domain
        guest.put("phone", null);  // Not currently stored in domain
        return guest;
    }

    private Map<String, Object> buildRoomItem(RoomReservationItem room) {
        Map<String, Object> item = new java.util.LinkedHashMap<>();
        item.put("roomItemId", room.getRoomItemId());
        item.put("roomCode", room.getRoomCode());
        item.put("checkInDate", room.getCheckInDate() != null ? room.getCheckInDate().toString() : null);
        item.put("checkOutDate", room.getCheckOutDate() != null ? room.getCheckOutDate().toString() : null);
        item.put("status", room.getStatus() != null ? room.getStatus().name() : null);
        item.put("basePrice", room.getBasePrice());
        item.put("cancellationFee", room.getCancellationFee());
        item.put("refundAmount", room.getRefundAmount());
        return item;
    }

    private Map<String, Object> buildFinancialBreakdown(FinancialBreakdown fb) {
        Map<String, Object> breakdown = new java.util.LinkedHashMap<>();
        breakdown.put("subtotal", fb.getSubtotal());
        breakdown.put("totalTax", fb.getTotalTax());
        breakdown.put("totalFees", fb.getTotalFees());
        breakdown.put("totalPenalties", fb.getTotalPenalties());
        breakdown.put("totalRefunds", fb.getTotalRefunds());
        breakdown.put("netTotal", fb.getNetTotal());
        return breakdown;
    }

    private Map<String, Object> buildHistoryEvent(ReservationHistoryEvent event) {
        Map<String, Object> e = new java.util.LinkedHashMap<>();
        e.put("eventId", event.getEventId());
        e.put("eventType", event.getEventType() != null ? event.getEventType().name() : null);
        e.put("actorId", event.getActorId());
        e.put("reason", event.getReason());
        e.put("occurredAt", event.getOccurredAt() != null ? event.getOccurredAt().toString() : null);
        return e;
    }
}
