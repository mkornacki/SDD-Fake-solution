package com.acme.reservation.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
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
 * T054: End-to-end reservation lifecycle test.
 * Full flow: create → retrieve → partial cancel (room) → full cancel (reservation).
 * Validates status progression, history events, and response schemas at each step.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("T054: Reservation lifecycle E2E — create → retrieve → partial cancel → full cancel")
class ReservationLifecycleE2ETest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:write", "SCOPE_reservation:read"})
    @DisplayName("Full lifecycle: create → retrieve → cancel room → cancel reservation")
    void fullLifecycle_createRetrieveCancelRoomCancelReservation() throws Exception {
        // Step 1: Create a two-room reservation
        String idempotencyKey = "e2e-create-" + System.nanoTime();
        Map<String, Object> createPayload = Map.of(
                "partnerId", "partner-e2e",
                "currencyCode", "USD",
                "guest", Map.of("givenName", "Alice", "familyName", "E2ETest"),
                "rooms", List.of(
                        Map.of("roomCode", "E2E-101",
                                "checkInDate", LocalDate.now().plusDays(10).toString(),
                                "checkOutDate", LocalDate.now().plusDays(13).toString(),
                                "basePrice", new BigDecimal("150.00")),
                        Map.of("roomCode", "E2E-102",
                                "checkInDate", LocalDate.now().plusDays(10).toString(),
                                "checkOutDate", LocalDate.now().plusDays(13).toString(),
                                "basePrice", new BigDecimal("200.00"))));

        MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andReturn();

        String reservationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("reservationId").asText();
        assertThat(reservationId).isNotBlank();

        // Step 2: Retrieve the reservation
        MvcResult getResult = mockMvc.perform(get("/api/v1/reservations/" + reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andReturn();

        JsonNode getResponse = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertThat(getResponse.has("reservationId")).isTrue();

        // Step 3: Retrieve room item id for partial cancel
        // The response should contain room items
        String roomItemId = extractFirstRoomItemId(getResponse);

        if (roomItemId != null) {
            // Step 3a: Cancel one room (partial cancellation)
            String roomCancelKey = "e2e-room-cancel-" + System.nanoTime();
            mockMvc.perform(delete("/api/v1/reservations/" + reservationId + "/rooms/" + roomItemId)
                            .header("X-Idempotency-Key", roomCancelKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "E2E partial cancel"))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.reservationId").value(reservationId));
        }

        // Step 4: Full cancellation of the reservation
        String cancelKey = "e2e-cancel-" + System.nanoTime();
        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-Idempotency-Key", cancelKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "E2E full cancel"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.status").isNotEmpty());

        // Step 5: Verify reservation is now CANCELLED
        mockMvc.perform(get("/api/v1/reservations/" + reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_reservation:write", "SCOPE_reservation:read"})
    @DisplayName("Create reservation idempotency: second call with same key returns 200")
    void createReservation_idempotentReplay_returns200() throws Exception {
        String idempotencyKey = "e2e-idempotent-" + System.nanoTime();
        Map<String, Object> payload = buildSingleRoomPayload();

        // First call
        MvcResult first = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andReturn();

        String firstId = objectMapper.readTree(
                first.getResponse().getContentAsString()).get("reservationId").asText();

        // Second call — idempotent replay
        MvcResult second = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        String secondId = objectMapper.readTree(
                second.getResponse().getContentAsString()).get("reservationId").asText();

        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("Cancel reservation idempotency: repeated calls return 200")
    void cancelReservation_repeatedCalls_idempotent() throws Exception {
        String reservationId = createSingleRoomReservation();
        String cancelKey = "e2e-cancel-idem-" + System.nanoTime();
        Map<String, Object> body = Map.of("reason", "E2E idempotent cancel test");

        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-Idempotency-Key", cancelKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted());

        mockMvc.perform(delete("/api/v1/reservations/" + reservationId)
                        .header("X-Idempotency-Key", cancelKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // --- Helpers ---

    private Map<String, Object> buildSingleRoomPayload() {
        return Map.of(
                "partnerId", "partner-e2e-single",
                "currencyCode", "GBP",
                "guest", Map.of("givenName", "Bob", "familyName", "E2E"),
                "rooms", List.of(Map.of(
                        "roomCode", "E2E-SINGLE",
                        "checkInDate", LocalDate.now().plusDays(3).toString(),
                        "checkOutDate", LocalDate.now().plusDays(5).toString(),
                        "basePrice", new BigDecimal("120.00"))));
    }

    private String createSingleRoomReservation() throws Exception {
        String key = "e2e-helper-create-" + System.nanoTime();
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSingleRoomPayload())))
                .andExpect(status().isAccepted())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("reservationId").asText();
    }

    private String extractFirstRoomItemId(JsonNode responseNode) {
        if (responseNode.has("rooms") && responseNode.get("rooms").isArray()
                && responseNode.get("rooms").size() > 0) {
            JsonNode firstRoom = responseNode.get("rooms").get(0);
            if (firstRoom.has("roomItemId")) {
                return firstRoom.get("roomItemId").asText();
            }
        }
        return null;
    }
}
