package com.acme.reservation;

import com.acme.reservation.adapters.outbound.audit.AuditEventSpringDataRepository;
import com.acme.reservation.adapters.outbound.audit.GovernanceAuditEventJpaEntity;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.RoomReservationItem;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T036: Audit trail integration test")
class AuditTrailIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AuditEventSpringDataRepository auditRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("Create and cancel flows emit audit_events with actor, traceId, action, and outcome")
    void createAndCancel_emitAuditEvents() throws Exception {
        String createTrace = "trace-audit-create-" + System.nanoTime();
        String cancelTrace = "trace-audit-cancel-" + System.nanoTime();

        MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "audit-create-" + System.nanoTime())
                        .header("X-Correlation-Id", createTrace)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreatePayload())))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        assertThat(body.get("reservationId").asText()).isNotBlank();

        String seededReservationId = seedReservation();

        mockMvc.perform(delete("/api/v1/reservations/" + seededReservationId)
                        .header("X-Idempotency-Key", "audit-cancel-" + System.nanoTime())
                        .header("X-Correlation-Id", cancelTrace)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "integration-test"))))
                .andExpect(status().isAccepted());

        List<GovernanceAuditEventJpaEntity> createEvents = auditRepository.findByTraceId(createTrace);
        List<GovernanceAuditEventJpaEntity> cancelEvents = auditRepository.findByTraceId(cancelTrace);

        assertThat(createEvents).isNotEmpty();
        assertThat(cancelEvents).isNotEmpty();

        createEvents.forEach(event -> {
            assertThat(event.getActorId()).isNotBlank();
            assertThat(event.getAction()).isNotBlank();
            assertThat(event.getOutcome()).isNotBlank();
            assertThat(event.getTraceId()).isEqualTo(createTrace);
        });

        cancelEvents.forEach(event -> {
            assertThat(event.getActorId()).isNotBlank();
            assertThat(event.getAction()).isNotBlank();
            assertThat(event.getOutcome()).isNotBlank();
            assertThat(event.getTraceId()).isEqualTo(cancelTrace);
        });
    }

    private Map<String, Object> buildCreatePayload() {
        return Map.of(
                "partnerId", "partner-audit-1",
                "currencyCode", "USD",
                "guest", Map.of("givenName", "Audit", "familyName", "User", "email", "audit@example.com"),
                "rooms", List.of(Map.of(
                        "roomCode", "AUD-101",
                        "checkInDate", "2026-08-01",
                        "checkOutDate", "2026-08-04",
                        "basePrice", 180.00)));
    }

                private String seedReservation() {
                RoomReservationItem room = RoomReservationItem.builder()
                    .roomCode("AUD-CANCEL-101")
                    .reservationId("placeholder")
                    .checkInDate(LocalDate.of(2026, 8, 10))
                    .checkOutDate(LocalDate.of(2026, 8, 12))
                    .basePrice(new BigDecimal("220.00"))
                    .build();

                Reservation initial = Reservation.create(
                    "partner-audit-1",
                    "guest:audit:user",
                    "USD",
                    "AUD-CANCEL-EXT",
                    List.of(room));

                RoomReservationItem fixedRoom = RoomReservationItem.builder()
                    .roomCode("AUD-CANCEL-101")
                    .reservationId(initial.getReservationId())
                    .checkInDate(LocalDate.of(2026, 8, 10))
                    .checkOutDate(LocalDate.of(2026, 8, 12))
                    .basePrice(new BigDecimal("220.00"))
                    .build();

                Reservation saved = reservationRepository.save(Reservation.create(
                    "partner-audit-1",
                    "guest:audit:user",
                    "USD",
                    "AUD-CANCEL-EXT",
                    List.of(fixedRoom)));
                return saved.getReservationId();
                }
}