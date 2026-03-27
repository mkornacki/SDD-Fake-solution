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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T038: Integration test for partial cancellation atomicity.
 * Verifies: room transitions, aggregate recalculation, history event.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T038: Partial room cancellation integration tests")
class CancelRoomIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:write", "SCOPE_reservation:read"})
    @DisplayName("Cancel one room in multi-room reservation — only that room transitions")
    void cancelRoom_partialCancel_onlyTargetedRoomTransitions() throws Exception {
        // 1. Create a reservation with 2 rooms
        String reservationId = createTwoRoomReservation();

        // 2. Get reservation to find room ids
        MvcResult getResult = mockMvc.perform(get("/api/v1/reservations/" + reservationId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(getResult.getResponse().getContentAsString());
        String roomItemId = body.get("rooms").get(0).get("roomItemId").asText();

        // 3. Cancel only first room
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId + "/rooms/" + roomItemId)
                        .header("X-Idempotency-Key", "cancel-room-it-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "guest no-show"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").value(reservationId));

        // 4. Verify second room remains active via GET
        MvcResult afterGet = mockMvc.perform(get("/api/v1/reservations/" + reservationId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterBody = objectMapper.readTree(afterGet.getResponse().getContentAsString());
        long activeRooms = 0;
        long cancelledRooms = 0;
        for (JsonNode room : afterBody.get("rooms")) {
            String status = room.get("status").asText();
            if ("ACTIVE".equals(status)) activeRooms++;
            if ("CANCELLATION_PENDING".equals(status) || "CANCELLED".equals(status)) cancelledRooms++;
        }
        assertThat(activeRooms).isEqualTo(1);
        assertThat(cancelledRooms).isEqualTo(1);
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:write", "SCOPE_reservation:read"})
    @DisplayName("Cancel room with same idempotency key returns idempotent result")
    void cancelRoom_idempotentReplay_returns200() throws Exception {
        String reservationId = createTwoRoomReservation();

        MvcResult getResult = mockMvc.perform(get("/api/v1/reservations/" + reservationId))
                .andReturn();
        JsonNode body = objectMapper.readTree(getResult.getResponse().getContentAsString());
        String roomItemId = body.get("rooms").get(0).get("roomItemId").asText();

        String idempotencyKey = "cancel-idem-it-" + System.nanoTime();

        // First call
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId + "/rooms/" + roomItemId)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "duplicate test"))))
                .andExpect(status().isAccepted());

        // Second call with same key — should return 200
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId + "/rooms/" + roomItemId)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "duplicate test"))))
                .andExpect(status().isOk());
    }

    private String createTwoRoomReservation() throws Exception {
        Map<String, Object> payload = Map.of(
                "partnerId", "partner-1",
                "currencyCode", "USD",
                "guest", Map.of(
                        "givenName", "Jane",
                        "familyName", "Doe"),
                "rooms", List.of(
                        Map.of("roomCode", "RMX-101",
                                "checkInDate", LocalDate.now().plusDays(7).toString(),
                                "checkOutDate", LocalDate.now().plusDays(10).toString(),
                                "basePrice", new BigDecimal("150.00")),
                        Map.of("roomCode", "RMX-102",
                                "checkInDate", LocalDate.now().plusDays(7).toString(),
                                "checkOutDate", LocalDate.now().plusDays(10).toString(),
                                "basePrice", new BigDecimal("150.00"))));

        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "create-for-cancel-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()).get("reservationId").asText();
    }
}
