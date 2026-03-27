package com.acme.reservation.adapters.http;

import com.acme.reservation.adapters.inbound.http.reservation.PIIAccessContext;
import com.acme.reservation.adapters.inbound.http.reservation.ReservationResponseMapper;
import com.acme.reservation.application.ports.inbound.GetReservationUseCase;
import com.acme.reservation.domain.financial.FinancialBreakdown;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T027: Unit test for PIIAccessContext masking logic.
 * Verifies that guest fields are masked when hasPIIAccess=false,
 * and revealed when hasPIIAccess=true.
 */
@DisplayName("T027: PII masking logic unit tests")
class PIIMaskingTest {

    private ReservationResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReservationResponseMapper();
    }

    @Test
    @DisplayName("PIIAccessContext: hasPIIAccess=false without pii:read scope")
    void piAccessContext_withoutPiiScope_hasNoPiiAccess() {
        PIIAccessContext ctx = new PIIAccessContext("user1", List.of("reservation:read", "reservation:write"));
        assertThat(ctx.hasPIIAccess()).isFalse();
    }

    @Test
    @DisplayName("PIIAccessContext: hasPIIAccess=true with pii:read scope")
    void piiAccessContext_withPiiScope_hasPiiAccess() {
        PIIAccessContext ctx = new PIIAccessContext("user1", List.of("pii:read", "reservation:read"));
        assertThat(ctx.hasPIIAccess()).isTrue();
    }

    @Test
    @DisplayName("PIIAccessContext: masks value when no PII access")
    void piiAccessContext_masksValue_withoutPiiAccess() {
        PIIAccessContext ctx = new PIIAccessContext("user1", List.of("reservation:read"));
        assertThat(ctx.maskIfNoAccess("John")).isEqualTo("***");
    }

    @Test
    @DisplayName("PIIAccessContext: returns value unchanged when PII access granted")
    void piiAccessContext_returnsValue_withPiiAccess() {
        PIIAccessContext ctx = new PIIAccessContext("user1", List.of("pii:read"));
        assertThat(ctx.maskIfNoAccess("John")).isEqualTo("John");
    }

    @Test
    @DisplayName("ReservationResponseMapper: guest fields masked when hasPiiAccess=false")
    void responseMapper_masksGuestFields_withoutPiiAccess() {
        GetReservationUseCase.Result result = buildMockResult();
        Map<String, Object> response = mapper.toResponse(result, false);

        @SuppressWarnings("unchecked")
        Map<String, Object> guest = (Map<String, Object>) response.get("guest");
        assertThat(guest.get("givenName")).isEqualTo("***");
        assertThat(guest.get("familyName")).isEqualTo("***");
        assertThat(guest.get("email")).isEqualTo("***");
        assertThat(guest.get("phone")).isEqualTo("***");
    }

    @Test
    @DisplayName("ReservationResponseMapper: guest fields visible when hasPiiAccess=true")
    void responseMapper_revealsGuestFields_withPiiAccess() {
        GetReservationUseCase.Result result = buildMockResult();
        Map<String, Object> response = mapper.toResponse(result, true);

        @SuppressWarnings("unchecked")
        Map<String, Object> guest = (Map<String, Object>) response.get("guest");
        assertThat(guest.get("givenName")).isEqualTo("Jane");
        assertThat(guest.get("familyName")).isEqualTo("Doe");
        assertThat((String) guest.get("givenName")).isNotEqualTo("***");
    }

    @Test
    @DisplayName("ReservationResponseMapper: non-PII reservation fields always visible")
    void responseMapper_nonPiiFields_alwaysVisible() {
        GetReservationUseCase.Result result = buildMockResult();
        Map<String, Object> unmasked = mapper.toResponse(result, false);

        assertThat(unmasked.get("reservationId")).isNotNull();
        assertThat(unmasked.get("partnerId")).isEqualTo("partner-1");
        assertThat(unmasked.get("status")).isEqualTo("PENDING");
        assertThat(unmasked.get("currencyCode")).isEqualTo("USD");
    }

    private GetReservationUseCase.Result buildMockResult() {
        RoomReservationItem room = RoomReservationItem.builder()
                .roomCode("RMX-101")
                .reservationId("res-001")
                .checkInDate(LocalDate.now().plusDays(7))
                .checkOutDate(LocalDate.now().plusDays(10))
                .basePrice(new BigDecimal("150.00"))
                .build();

        Reservation reservation = Reservation.create(
                "partner-1",
                "guest:Jane:Doe",
                "USD",
                "EXT-001",
                List.of(room));

        FinancialBreakdown fb = new FinancialBreakdown(
                null, reservation.getReservationId(),
                List.of(),
                new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());

        List<ReservationHistoryEvent> history = List.of(
                ReservationHistoryEvent.builder()
                        .eventId(java.util.UUID.randomUUID().toString())
                        .reservationId(reservation.getReservationId())
                        .eventType(ReservationHistoryEvent.EventType.CREATED)
                        .actorId("user1")
                        .actorType(ReservationHistoryEvent.ActorType.USER)
                        .traceId("trace-001")
                        .occurredAt(Instant.now())
                        .build());

        return new GetReservationUseCase.Result(reservation, history, fb);
    }
}
