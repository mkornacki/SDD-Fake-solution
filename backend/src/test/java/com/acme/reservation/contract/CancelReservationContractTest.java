package com.acme.reservation.contract;

import com.acme.reservation.application.ports.inbound.CancelReservationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T045: Contract test (provider-side) for DELETE /api/v1/reservations/{reservationId}.
 * Verifies 202 for new cancellations and 200 for idempotent replays.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T045: Cancel reservation contract tests")
class CancelReservationContractTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CancelReservationUseCase cancelReservationUseCase;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("DELETE reservation returns 202 Accepted for new cancellation")
    void deleteReservation_newCancellation_returns202() throws Exception {
        when(cancelReservationUseCase.execute(any()))
                .thenReturn(new CancelReservationUseCase.Result("res-001", "CANCELLED", false));

        mockMvc.perform(delete("/api/v1/reservations/res-001")
                        .header("X-Idempotency-Key", "cancel-res-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "guest request"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").value("res-001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("DELETE reservation returns 200 for idempotent replay")
    void deleteReservation_idempotentReplay_returns200() throws Exception {
        when(cancelReservationUseCase.execute(any()))
                .thenReturn(new CancelReservationUseCase.Result("res-001", "CANCELLED", true));

        mockMvc.perform(delete("/api/v1/reservations/res-001")
                        .header("X-Idempotency-Key", "cancel-res-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "guest request"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value("res-001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("DELETE reservation without idempotency key returns 400")
    void deleteReservation_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/reservations/res-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "guest request"))))
                .andExpect(status().isBadRequest());
    }
}
