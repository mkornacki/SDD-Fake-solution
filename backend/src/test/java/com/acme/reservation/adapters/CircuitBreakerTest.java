package com.acme.reservation.adapters;

import com.acme.reservation.adapters.outbound.messaging.DlqRouter;
import com.acme.reservation.adapters.outbound.partner.PartnerIntegrationWorker;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import com.acme.reservation.domain.audit.IntegrationTask;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("T041: Circuit-breaker behavior tests")
class CircuitBreakerTest {

    @Autowired
    private PartnerIntegrationWorker worker;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private IntegrationTaskRepository integrationTaskRepository;

    @MockBean
    private RequestContextRepository requestContextRepository;

    @MockBean
    private DlqRouter dlqRouter;

    @Test
    @DisplayName("Circuit breaker opens after configured failure threshold and worker falls back to retry wait")
    void circuitBreaker_opensAndWorkerFallsBack() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("partnerIntegration");
        circuitBreaker.reset();

        for (int index = 0; index < 10; index++) {
            circuitBreaker.onError(0L, TimeUnit.MILLISECONDS, new RuntimeException("partner down"));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        IntegrationTask task = new IntegrationTask(
                "res-cb-1",
                null,
                IntegrationTask.TaskType.PARTNER_CREATE,
                5);

        when(integrationTaskRepository.findPendingTasks()).thenReturn(List.of(task));
        when(integrationTaskRepository.save(any(IntegrationTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        worker.processPendingTasks();

        assertThat(task.getState()).isEqualTo(IntegrationTask.TaskState.RETRY_WAIT);
        assertThat(task.getFailureClass()).isEqualTo(IntegrationTask.FailureClass.TRANSIENT);
    }
}