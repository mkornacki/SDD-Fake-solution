package com.acme.reservation.contract;

import com.acme.reservation.adapters.inbound.http.reservation.ReservationResponseMapper;
import com.acme.reservation.application.ports.inbound.GetReservationUseCase;
import com.acme.reservation.domain.financial.FinancialBreakdown;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationHistoryEvent;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T028: Contract test (provider-side) for GET /api/v1/reservations/{reservationId}.
 * Verifies response schema with and without PII scope.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T028: GET reservation contract tests")
class GetReservationContractTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    GetReservationUseCase getReservationUseCase;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:read")
    @DisplayName("GET reservation without pii:read scope — guest fields are masked")
    void get_reservation_withoutPiiScope_masksGuestFields() throws Exception {
        when(getReservationUseCase.execute(any()))
                .thenReturn(buildMockResult());

        mockMvc.perform(get("/api/v1/reservations/res-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.partnerId").value("partner-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.guest.givenName").value("***"))
                .andExpect(jsonPath("$.guest.familyName").value("***"))
                .andExpect(jsonPath("$.guest.email").value("***"))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0].roomCode").value("RMX-101"));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:read", "SCOPE_pii:read"})
    @DisplayName("GET reservation with pii:read scope — guest fields are visible")
    void get_reservation_withPiiScope_revealsGuestFields() throws Exception {
        when(getReservationUseCase.execute(any()))
                .thenReturn(buildMockResult());

        mockMvc.perform(get("/api/v1/reservations/res-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guest.givenName").value("Jane"))
                .andExpect(jsonPath("$.guest.familyName").value("Doe"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:read")
    @DisplayName("GET reservation includes rooms, history, and financial breakdown in response")
    void get_reservation_includesFullResponseSchema() throws Exception {
        when(getReservationUseCase.execute(any()))
                .thenReturn(buildMockResult());

        mockMvc.perform(get("/api/v1/reservations/res-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.financialBreakdown.subtotal").value(150.0))
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history[0].eventType").value("CREATED"));
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
                UUID.randomUUID().toString(), reservation.getReservationId(),
                List.of(),
                new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());

        List<ReservationHistoryEvent> history = List.of(
                ReservationHistoryEvent.builder()
                        .eventId(UUID.randomUUID().toString())
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
