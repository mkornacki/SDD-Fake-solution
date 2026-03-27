package com.acme.reservation.e2e;

import com.acme.reservation.application.ports.outbound.DlqRepository;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.application.ports.outbound.ReservationRepository;
import com.acme.reservation.domain.audit.DlqItem;
import com.acme.reservation.domain.audit.IntegrationTask;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T049: Idempotent create and replay E2E")
class IdempotentCreateE2ETest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DlqRepository dlqRepository;

        @Autowired
        IntegrationTaskRepository integrationTaskRepository;

        @Autowired
        ReservationRepository reservationRepository;

    @Test
        @SuppressWarnings("null")
    @DisplayName("Concurrent retries produce one reservation, then DLQ replay succeeds")
    void concurrentRetries_thenDlqReplay() throws Exception {
        String idempotencyKey = "e2e-retry-" + System.nanoTime();
        String correlationId = "trace-t049-" + System.nanoTime();
        String payload = objectMapper.writeValueAsString(buildCreatePayload());

        int concurrentRequests = 6;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);

        List<Callable<MvcResult>> tasks = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            tasks.add(() -> {
                startLatch.await();
                return mockMvc.perform(post("/api/v1/reservations")
                                .with(SecurityMockMvcRequestPostProcessors.user("partner")
                                        .authorities(new SimpleGrantedAuthority("SCOPE_reservation:write")))
                                .header("X-Idempotency-Key", idempotencyKey)
                                .header("X-Correlation-Id", correlationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andReturn();
            });
        }

        List<Future<MvcResult>> futures = tasks.stream().map(executor::submit).collect(java.util.stream.Collectors.toList());
        startLatch.countDown();

        Set<String> reservationIds = new HashSet<>();
        for (Future<MvcResult> future : futures) {
                        MvcResult result = future.get();
                        int statusCode = result.getResponse().getStatus();
                        if (statusCode >= 200 && statusCode < 300) {
                                JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                                reservationIds.add(body.get("reservationId").asText());
                        }
        }

        executor.shutdownNow();
        assertThat(reservationIds).isNotEmpty();
        assertThat(reservationIds).hasSize(1);

        Reservation dlqReservation = seedReservation("t049-dlq-fixture");
        IntegrationTask task = integrationTaskRepository.save(new IntegrationTask(
                dlqReservation.getReservationId(),
                null,
                IntegrationTask.TaskType.PARTNER_CREATE,
                3));

        DlqItem dlqItem = new DlqItem(
                task.getTaskId(),
                dlqReservation.getReservationId(),
                "partner-timeout",
                "attempt-ref",
                "masked-payload-ref");
        DlqItem saved = dlqRepository.save(dlqItem);

        mockMvc.perform(post("/api/v1/operations/dlq/{dlqId}/replay", saved.getDlqId())
                        .with(SecurityMockMvcRequestPostProcessors.user("ops-admin")
                                .authorities(new SimpleGrantedAuthority("SCOPE_admin:dlq")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "operator replay"))))
                .andExpect(status().isAccepted());
    }

    private Map<String, Object> buildCreatePayload() {
        return Map.of(
                "partnerId", "partner-t049",
                "currencyCode", "USD",
                "externalReference", "ext-" + System.nanoTime(),
                "guest", Map.of("givenName", "Retry", "familyName", "User"),
                "rooms", List.of(Map.of(
                        "roomCode", "T049-101",
                        "checkInDate", LocalDate.now().plusDays(5).toString(),
                        "checkOutDate", LocalDate.now().plusDays(7).toString(),
                        "basePrice", new BigDecimal("123.45"))));
    }

    private Reservation seedReservation(String externalReference) {
        Reservation created = Reservation.create(
                "partner-t049",
                "guest:T049:Fixture",
                "USD",
                externalReference,
                List.of(RoomReservationItem.builder()
                        .reservationId("seed-reservation")
                        .roomCode("T049-DLQ-101")
                        .checkInDate(LocalDate.now().plusDays(6))
                        .checkOutDate(LocalDate.now().plusDays(8))
                        .basePrice(new BigDecimal("140.00"))
                        .build()));

        return reservationRepository.save(Reservation.create(
                created.getPartnerId(),
                created.getGuestId(),
                created.getCurrencyCode(),
                created.getExternalReference(),
                List.of(RoomReservationItem.builder()
                        .reservationId(created.getReservationId())
                        .roomCode("T049-DLQ-101")
                        .checkInDate(LocalDate.now().plusDays(6))
                        .checkOutDate(LocalDate.now().plusDays(8))
                        .basePrice(new BigDecimal("140.00"))
                        .build())));
    }
}
