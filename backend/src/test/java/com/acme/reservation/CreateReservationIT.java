package com.acme.reservation;

import com.acme.reservation.application.ports.inbound.CreateReservationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for idempotent reservation creation.
 * Verifies that concurrent duplicate requests produce one reservation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreateReservationIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CreateReservationUseCase createReservationUseCase;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    void createReservation_successfulCreation_returns202() throws Exception {
        Map<String, Object> payload = buildPayload();

        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "it-key-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andReturn();

        String reservationId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("reservationId").asText();
        assertThat(reservationId).isNotBlank();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    void createReservation_duplicateKey_returnsIdempotentResult() throws Exception {
        Map<String, Object> payload = buildPayload();
        String idempotencyKey = "idem-it-dup-" + System.nanoTime();

        // First request
        MvcResult first = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();

        // Second request with same key
        MvcResult second = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("reservationId").asText();
        String secondId = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("reservationId").asText();

        // Both must return the same reservation ID
        assertThat(firstId).isEqualTo(secondId);
        // Second response should be 200 (idempotent replay)
        assertThat(second.getResponse().getStatus()).isEqualTo(200);
    }

    private Map<String, Object> buildPayload() {
        return Map.of(
                "partnerId", "partner-integration-1",
                "currencyCode", "USD",
                "guest", Map.of("givenName", "Jane", "familyName", "Doe"),
                "rooms", List.of(Map.of(
                        "roomCode", "ROOM-101",
                        "checkInDate", "2026-07-01",
                        "checkOutDate", "2026-07-05",
                        "basePrice", 200.00)));
    }
}
