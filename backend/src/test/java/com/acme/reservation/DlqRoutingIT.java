package com.acme.reservation;

import com.acme.reservation.adapters.outbound.messaging.DlqRouter;
import com.acme.reservation.adapters.outbound.partner.PartnerIntegrationWorker;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import com.acme.reservation.domain.audit.IntegrationTask;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T026: Integration test for DLQ routing after retry exhaustion.
 */
@SpringBootTest
@ActiveProfiles("test")
class DlqRoutingIT {

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

    @BeforeEach
    void setup() {
        circuitBreakerRegistry.circuitBreaker("partnerIntegration").transitionToOpenState();
    }

    @Test
    @DisplayName("After max attempts exhausted, task is routed to DLQ")
    void exhaustedRetries_areRoutedToDlq() {
        IntegrationTask task = new IntegrationTask(
                "task-1",
                "res-1",
                null,
                IntegrationTask.TaskType.PARTNER_CREATE,
                IntegrationTask.TaskState.READY,
                2,
                2,
                Instant.now().minusSeconds(1),
                "last-failure",
                IntegrationTask.FailureClass.TRANSIENT,
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(1));

        when(integrationTaskRepository.findPendingTasks()).thenReturn(List.of(task));
        when(integrationTaskRepository.save(any(IntegrationTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        worker.processPendingTasks();

        verify(integrationTaskRepository, atLeast(1)).save(any(IntegrationTask.class));
        verify(dlqRouter).route(any(IntegrationTask.class), contains("MAX_RETRIES_EXCEEDED"));
    }
}
