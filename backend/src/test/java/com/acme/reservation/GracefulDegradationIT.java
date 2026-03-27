package com.acme.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T043: Graceful degradation integration test")
class GracefulDegradationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("partnerIntegration").transitionToOpenState();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:write")
    @DisplayName("Create acknowledgement remains available when partner path is degraded")
    void createReservation_returnsAcceptedWhenPartnerDegraded() throws Exception {
        Map<String, Object> payload = Map.of(
                "partnerId", "partner-degraded-1",
                "currencyCode", "USD",
                "guest", Map.of("givenName", "Grace", "familyName", "Degrade"),
                "rooms", List.of(Map.of(
                        "roomCode", "DEG-101",
                        "checkInDate", "2026-09-01",
                        "checkOutDate", "2026-09-03",
                        "basePrice", 199.00)));

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-Idempotency-Key", "graceful-" + System.nanoTime())
                        .header("X-Correlation-Id", "graceful-trace-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted());
    }
}