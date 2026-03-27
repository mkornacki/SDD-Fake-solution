package com.acme.reservation.e2e;

import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.RoomReservationItem;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T051: Resilience security E2E")
class ResilienceSecurityE2ETest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

        @Autowired
        ReservationRepository reservationRepository;

    @Test
    @DisplayName("Unauthorized DLQ replay is denied")
    @SuppressWarnings("null")
    void replayWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/operations/dlq/any-id/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "attempt"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("DLQ replay requires admin scope")
    @SuppressWarnings("null")
    void replayWithoutAdminScopeReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/operations/dlq/any-id/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "attempt"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ops-admin", authorities = "SCOPE_admin:dlq")
    @DisplayName("Replay reason must be non-empty")
    @SuppressWarnings("null")
    void replayWithBlankReasonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/operations/dlq/any-id/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:write", "SCOPE_reservation:read"})
    @DisplayName("PII is masked without pii:read scope")
    void piiMaskedWithoutPiiReadScope() throws Exception {
        String reservationId = seedReservation("Mask", "Me");

        String responseBody = mockMvc.perform(get("/api/v1/reservations/{reservationId}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guest.givenName").value("***"))
                .andExpect(jsonPath("$.guest.familyName").value("***"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("Mask");
        assertThat(responseBody).doesNotContain("Me");
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:write", "SCOPE_reservation:read", "SCOPE_pii:read"})
    @DisplayName("PII is visible with pii:read scope")
    void piiVisibleWithPiiReadScope() throws Exception {
        String reservationId = seedReservation("Visible", "Guest");

        mockMvc.perform(get("/api/v1/reservations/{reservationId}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guest.givenName").value("Visible"))
                .andExpect(jsonPath("$.guest.familyName").value("Guest"));
    }

    private String seedReservation(String givenName, String familyName) {
        Reservation created = Reservation.create(
                "partner-security",
                "guest:" + givenName + ":" + familyName,
                "USD",
                "sec-ext-" + System.nanoTime(),
                java.util.List.of(RoomReservationItem.builder()
                        .reservationId("seed-reservation")
                        .roomCode("SEC-101")
                        .checkInDate(LocalDate.now().plusDays(3))
                        .checkOutDate(LocalDate.now().plusDays(5))
                        .basePrice(new BigDecimal("99.00"))
                        .build()));

        Reservation saved = reservationRepository.save(Reservation.create(
                created.getPartnerId(),
                created.getGuestId(),
                created.getCurrencyCode(),
                created.getExternalReference(),
                java.util.List.of(RoomReservationItem.builder()
                        .reservationId(created.getReservationId())
                        .roomCode("SEC-101")
                        .checkInDate(LocalDate.now().plusDays(3))
                        .checkOutDate(LocalDate.now().plusDays(5))
                        .basePrice(new BigDecimal("99.00"))
                        .build())));
        return saved.getReservationId();
    }
}
