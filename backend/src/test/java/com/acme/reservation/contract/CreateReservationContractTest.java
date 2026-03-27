package com.acme.reservation.contract;

import com.acme.reservation.application.ports.inbound.CreateReservationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test (provider-side): POST /api/v1/reservations
 * Verifies 202 Accepted and idempotent 200 replay response contracts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreateReservationContractTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CreateReservationUseCase createReservationUseCase;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    void post_reservations_returns202_withNewReservation() throws Exception {
        when(createReservationUseCase.execute(any()))
                .thenReturn(new CreateReservationUseCase.Result(
                        "res-abc-123", "PENDING", "QUEUED", false));

        Map<String, Object> payload = buildPayload();

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "idem-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").value("res-abc-123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.partnerProcessingStatus").value("QUEUED"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    void post_reservations_returns200_onIdempotentReplay() throws Exception {
        when(createReservationUseCase.execute(any()))
                .thenReturn(new CreateReservationUseCase.Result(
                        "res-abc-123", "ACTIVE", "COMPLETED", true));

        Map<String, Object> payload = buildPayload();

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "idem-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value("res-abc-123"));
    }

    @Test
    void post_reservations_returns401_withoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "idem-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    void post_reservations_returns400_withoutIdempotencyKey() throws Exception {
        Map<String, Object> payload = buildPayload();

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    private Map<String, Object> buildPayload() {
        return Map.of(
                "partnerId", "partner-1",
                "currencyCode", "USD",
                "guest", Map.of("givenName", "John", "familyName", "Doe"),
                "rooms", java.util.List.of(Map.of(
                        "roomCode", "ROOM-101",
                        "checkInDate", "2026-06-01",
                        "checkOutDate", "2026-06-05",
                        "basePrice", 150.00)));
    }
}
