package com.acme.reservation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T042: Metrics exposure integration test")
class MetricsExposureIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "SCOPE_reservation:read")
    @DisplayName("Actuator metrics exposes reservation queue, retry, DLQ, and idempotency metrics")
    void actuatorMetrics_exposesCustomReservationMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray())
                .andExpect(jsonPath("$.names[?(@ == 'reservation.queue.depth')]").isNotEmpty())
                .andExpect(jsonPath("$.names[?(@ == 'reservation.consumer.lag.seconds')]").isNotEmpty())
                .andExpect(jsonPath("$.names[?(@ == 'reservation.retry.count')]").isNotEmpty())
                .andExpect(jsonPath("$.names[?(@ == 'reservation.dlq.growth')]").isNotEmpty())
                .andExpect(jsonPath("$.names[?(@ == 'reservation.idempotency.hit.ratio')]").isNotEmpty())
                .andExpect(jsonPath("$.names[?(@ == 'reservation.create.latency')]").isNotEmpty())
                .andExpect(jsonPath("$.names[?(@ == 'reservation.partner.integration.latency')]").isNotEmpty());
    }
}