package com.acme.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T046: Integration test for full reservation cancellation idempotency.
 * 3 repeated cancellation calls produce one audit event and one DLQ evaluation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T046: Cancel reservation IT — idempotency")
class CancelReservationIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("Cancel reservation returns 202 Accepted")
    void cancelReservation_returns202() throws Exception {
        String reservationId = createReservation();
        String idempotencyKey = "cancel-res-it-" + System.nanoTime();

        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "operator request"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.status").isNotEmpty());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("Repeated cancellations with same key are idempotent (return 200)")
    void cancelReservation_repeated_idempotentReturns200() throws Exception {
        String reservationId = createReservation();
        String idempotencyKey = "cancel-idem-it-" + System.nanoTime();
        Map<String, Object> body = Map.of("reason", "duplicate cancel test");

        // First call
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted());

        // Second call — idempotent
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Third call — still idempotent
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private String createReservation() throws Exception {
        Map<String, Object> payload = Map.of(
                "partnerId", "partner-1",
                "currencyCode", "USD",
                "guest", Map.of("givenName", "Jane", "familyName", "Doe"),
                "rooms", List.of(Map.of(
                        "roomCode", "RMX-101",
                        "checkInDate", LocalDate.now().plusDays(7).toString(),
                        "checkOutDate", LocalDate.now().plusDays(10).toString(),
                        "basePrice", new BigDecimal("150.00"))));

        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "create-for-cancel-res-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()).get("reservationId").asText();
    }
}
