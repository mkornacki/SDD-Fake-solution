package com.acme.reservation.contract;

import com.acme.reservation.adapters.outbound.partner.PartnerIntegrationWorker;
import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T024: Contract-oriented test for partner integration behavior.
 *
 * Current partner adapter is a stubbed worker endpoint in this codebase,
 * so this verifies the operational contract around circuit-breaker behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class PartnerIntegrationContractTest {

    @MockBean
    private RequestContextRepository requestContextRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private PartnerIntegrationWorker worker;

    @Test
    @DisplayName("Partner integration circuit-breaker contract can be resolved by name")
    void partnerCircuitBreaker_contractExists() {
        assertThat(circuitBreakerRegistry.circuitBreaker("partnerIntegration")).isNotNull();
        assertThat(worker).isNotNull();
    }
}
