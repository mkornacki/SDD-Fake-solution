package com.acme.reservation.adapters.outbound.messaging;

import com.acme.reservation.application.ports.outbound.DlqRepository;
import com.acme.reservation.domain.audit.DlqItem;
import com.acme.reservation.domain.audit.IntegrationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * T052: Routes terminally failed IntegrationTasks to the DLQ for operator triage.
 * Stores a masked payload reference (no raw PII) and full attempt history reference.
 */
@Service
public class DlqRouter {

    private static final Logger LOG = LoggerFactory.getLogger(DlqRouter.class);

    private final DlqRepository dlqRepository;

    public DlqRouter(DlqRepository dlqRepository) {
        this.dlqRepository = dlqRepository;
    }

    /**
     * Routes a terminally failed task to the DLQ.
     *
     * @param task          the failed IntegrationTask
     * @param failureReason a descriptive reason for the terminal failure
     */
    public DlqItem route(IntegrationTask task, String failureReason) {
        String attemptHistoryRef = buildAttemptHistoryRef(task);
        String maskedPayloadRef = buildMaskedPayloadRef(task);

        DlqItem dlqItem = new DlqItem(
                task.getTaskId(),
                task.getReservationId(),
                failureReason,
                attemptHistoryRef,
                maskedPayloadRef);

        DlqItem saved = dlqRepository.save(dlqItem);
        LOG.warn("Task {} routed to DLQ as dlq_id={} after {} attempts. Reason: {}",
                task.getTaskId(), saved.getDlqId(), task.getAttemptCount(), failureReason);
        return saved;
    }

    private String buildAttemptHistoryRef(IntegrationTask task) {
        return String.format("task/%s/attempts/%d", task.getTaskId(), task.getAttemptCount());
    }

    private String buildMaskedPayloadRef(IntegrationTask task) {
        // Reference only — never stored raw payload; PII is already tokenised upstream
        return String.format("tasks/%s/payload-ref", task.getTaskId());
    }
}
