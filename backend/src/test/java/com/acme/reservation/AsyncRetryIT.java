package com.acme.reservation;

import com.acme.reservation.adapters.outbound.messaging.DlqRouter;
import com.acme.reservation.adapters.outbound.partner.PartnerIntegrationWorker;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import com.acme.reservation.domain.audit.IntegrationTask;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T025: Integration test for transient retry progression.
 */
@SpringBootTest
@ActiveProfiles("test")
class AsyncRetryIT {

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
        circuitBreakerRegistry.circuitBreaker("partnerIntegration").reset();
    }

    @Test
    @DisplayName("Transient failure enters RETRY_WAIT then eventually SUCCEEDED")
    void transientRetryProgression_eventuallySucceeds() {
        IntegrationTask task = new IntegrationTask(
                "res-1",
                null,
                IntegrationTask.TaskType.PARTNER_CREATE,
                5);
        List<IntegrationTask.TaskState> persistedStates = new ArrayList<>();

        AtomicInteger poll = new AtomicInteger(0);
        when(integrationTaskRepository.findPendingTasks()).thenAnswer(invocation -> {
            int call = poll.incrementAndGet();
            if (call == 1) {
                circuitBreakerRegistry.circuitBreaker("partnerIntegration").transitionToOpenState();
                return List.of(task);
            }
            circuitBreakerRegistry.circuitBreaker("partnerIntegration").reset();
            if (task.getState() == IntegrationTask.TaskState.RETRY_WAIT) {
                task.markRetryWait("due", Instant.now().minusSeconds(1), Instant.now());
            }
            return List.of(task);
        });

        when(integrationTaskRepository.save(any(IntegrationTask.class)))
            .thenAnswer(invocation -> {
                IntegrationTask current = invocation.getArgument(0);
                persistedStates.add(current.getState());
                return current;
            });

        worker.processPendingTasks();
        worker.processPendingTasks();

        verify(integrationTaskRepository, atLeast(2)).save(any(IntegrationTask.class));

        boolean hadRetryWait = persistedStates.stream()
            .anyMatch(state -> state == IntegrationTask.TaskState.RETRY_WAIT);
        boolean hadSucceeded = persistedStates.stream()
            .anyMatch(state -> state == IntegrationTask.TaskState.SUCCEEDED);

        assertThat(hadRetryWait).isTrue();
        assertThat(hadSucceeded).isTrue();
    }
}
