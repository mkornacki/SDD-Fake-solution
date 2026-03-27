package com.acme.reservation.contract;

import com.acme.reservation.application.ports.inbound.CancelRoomUseCase;
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
 * T037: Contract test (provider-side) for DELETE /api/v1/reservations/{id}/rooms/{roomId}.
 * Verifies 202 for new cancellations and 200 for idempotent replays.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T037: Cancel room contract tests")
class CancelRoomContractTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CancelRoomUseCase cancelRoomUseCase;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("DELETE room returns 202 Accepted for new cancellation")
    void deleteRoom_newCancellation_returns202() throws Exception {
        when(cancelRoomUseCase.execute(any()))
                .thenReturn(new CancelRoomUseCase.Result(
                        "res-001", "room-item-001", "CANCELLATION_PENDING", false));

        mockMvc.perform(delete("/api/v1/reservations/res-001/rooms/room-item-001")
                        .header("X-Idempotency-Key", "cancel-room-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "guest request"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").value("res-001"))
                .andExpect(jsonPath("$.status").value("CANCELLATION_PENDING"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("DELETE room returns 200 for idempotent replay")
    void deleteRoom_idempotentReplay_returns200() throws Exception {
        when(cancelRoomUseCase.execute(any()))
                .thenReturn(new CancelRoomUseCase.Result(
                        "res-001", "room-item-001", "CANCELLED", true));

        mockMvc.perform(delete("/api/v1/reservations/res-001/rooms/room-item-001")
                        .header("X-Idempotency-Key", "cancel-room-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "guest request"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value("res-001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("DELETE room missing idempotency key returns 400")
    void deleteRoom_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/reservations/res-001/rooms/room-item-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "guest request"))))
                .andExpect(status().isBadRequest());
    }
}
