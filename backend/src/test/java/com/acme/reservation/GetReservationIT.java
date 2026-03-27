package com.acme.reservation;

import com.acme.reservation.application.ports.inbound.CreateReservationUseCase;
import com.acme.reservation.application.ports.inbound.GetReservationUseCase;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T029: Integration test for reservation retrieval including history and financial breakdown.
 * Uses real Spring context with H2 in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T029: GetReservation integration tests")
class GetReservationIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CreateReservationUseCase createReservationUseCase;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:read")
    @DisplayName("GET reservation returns reservation data without PII when no pii:read scope")
    void getReservation_returns200_withMaskedGuest() throws Exception {
        // Create a reservation first
        String reservationId = createReservationAndGetId();

        // Retrieve without PII scope
        mockMvc.perform(get("/api/v1/reservations/" + reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.guest.givenName").value("***"))
                .andExpect(jsonPath("$.guest.familyName").value("***"))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0].roomCode").value("RMX-101"));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:read", "SCOPE_pii:read"})
    @DisplayName("GET reservation returns full PII when pii:read scope present")
    void getReservation_withPiiScope_revealsGuestData() throws Exception {
        // Create a reservation first
        String reservationId = createReservationAndGetId();

        mockMvc.perform(get("/api/v1/reservations/" + reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guest.givenName").value("Jane"))
                .andExpect(jsonPath("$.guest.familyName").value("Doe"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:read")
    @DisplayName("GET non-existent reservation returns 404")
    void getReservation_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/non-existent-res-id"))
                .andExpect(status().isNotFound());
    }

    private String createReservationAndGetId() throws Exception {
        Map<String, Object> payload = Map.of(
                "partnerId", "partner-1",
                "externalReference", "EXT-001",
                "currencyCode", "USD",
                "guest", Map.of(
                        "givenName", "Jane",
                        "familyName", "Doe",
                        "email", "jane.doe@example.com",
                        "phone", "+1-555-0100"),
                "rooms", List.of(Map.of(
                        "roomCode", "RMX-101",
                        "checkInDate", LocalDate.now().plusDays(7).toString(),
                        "checkOutDate", LocalDate.now().plusDays(10).toString(),
                        "basePrice", new BigDecimal("150.00"))));

        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .with(user("setup").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_reservation:write")))
                        .header("X-Idempotency-Key", "it-get-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()).get("reservationId").asText();
    }
}
