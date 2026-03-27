package com.acme.reservation.e2e;

import com.acme.reservation.application.ports.outbound.DlqRepository;
import com.acme.reservation.application.ports.outbound.GovernanceAuditEventRepository;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.domain.audit.AuditEvent;
import com.acme.reservation.domain.audit.DlqItem;
import com.acme.reservation.domain.audit.IntegrationTask;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T050: DLQ replay audit E2E")
class DlqReplayE2ETest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DlqRepository dlqRepository;

        @Autowired
        ReservationRepository reservationRepository;

        @Autowired
        IntegrationTaskRepository integrationTaskRepository;

    @Autowired
    GovernanceAuditEventRepository governanceAuditEventRepository;

    @Test
    @WithMockUser(username = "ops-admin", authorities = "SCOPE_admin:dlq")
        @SuppressWarnings("null")
    @DisplayName("Replay captures operator identity, reason, and trace in governance audit")
    void replayCapturesOperatorReasonAndTrace() throws Exception {
        Reservation reservation = seedReservation("reservation-t050");
        IntegrationTask task = integrationTaskRepository.save(
                new IntegrationTask(
                        reservation.getReservationId(),
                        null,
                        IntegrationTask.TaskType.PARTNER_CREATE,
                        3));

        DlqItem seeded = dlqRepository.save(new DlqItem(
                task.getTaskId(),
                reservation.getReservationId(),
                "terminal-failure",
                "attempt-history",
                "masked-payload"));

        String traceId = "trace-t050-" + System.nanoTime();
        String replayReason = "partner recovered";

        mockMvc.perform(post("/api/v1/operations/dlq/{dlqId}/replay", seeded.getDlqId())
                        .header("X-Correlation-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", replayReason))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dlqId").value(seeded.getDlqId()))
                .andExpect(jsonPath("$.replayStatus").value("REPLAYED"))
                .andExpect(jsonPath("$.replayCount").value(1));

        List<AuditEvent> auditEvents = governanceAuditEventRepository.findByEntityIdAndType(
                seeded.getDlqId(),
                "DLQ_ITEM");

        assertThat(auditEvents).isNotEmpty();
        AuditEvent last = auditEvents.get(auditEvents.size() - 1);
        assertThat(last.getActorId()).isEqualTo("ops-admin");
        assertThat(last.getAction()).isEqualTo("DLQ_REPLAY_REQUESTED");
        assertThat(last.getTraceId()).isEqualTo(traceId);
        assertThat(last.getAfterRef()).contains(replayReason);
    }

        private Reservation seedReservation(String externalReference) {
                Reservation reservation = Reservation.create(
                                "partner-t050",
                                "guest:T050:Operator",
                                "USD",
                                externalReference,
                                List.of(RoomReservationItem.builder()
                                                .reservationId("seed-reservation")
                                                .roomCode("T050-101")
                                                .checkInDate(LocalDate.now().plusDays(2))
                                                .checkOutDate(LocalDate.now().plusDays(4))
                                                .basePrice(new BigDecimal("175.00"))
                                                .build()));

                return reservationRepository.save(Reservation.create(
                                reservation.getPartnerId(),
                                reservation.getGuestId(),
                                reservation.getCurrencyCode(),
                                reservation.getExternalReference(),
                                List.of(RoomReservationItem.builder()
                                                .reservationId(reservation.getReservationId())
                                                .roomCode("T050-101")
                                                .checkInDate(LocalDate.now().plusDays(2))
                                                .checkOutDate(LocalDate.now().plusDays(4))
                                                .basePrice(new BigDecimal("175.00"))
                                                .build())));
        }
}
