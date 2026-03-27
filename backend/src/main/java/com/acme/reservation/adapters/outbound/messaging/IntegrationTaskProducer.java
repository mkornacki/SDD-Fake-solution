package com.acme.reservation.adapters.outbound.messaging;

import com.acme.reservation.domain.audit.IntegrationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * T049: Outbound adapter that enqueues IntegrationTask notifications to the durable queue.
 * Uses Spring AMQP RabbitTemplate when RabbitMQ is available; falls back gracefully to
 * DB-polling-only mode when the broker is not reachable (test / infra-less environments).
 */
@Service
public class IntegrationTaskProducer {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTaskProducer.class);

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Value("${reservation.async-worker.queue-name:reservation.integration.tasks}")
    private String queueName;

    /**
     * Notify the durable queue that a new task is ready for processing.
     * The task has already been persisted via IntegrationTaskRepository by the caller;
     * this message is a fast-path signal for the worker — if delivery fails the
     * scheduled DB poller will still pick it up.
     *
     * @param task the persisted IntegrationTask to enqueue
     */
    public void enqueue(IntegrationTask task) {
        if (rabbitTemplate == null) {
            LOG.debug("RabbitMQ not configured — task {} will be picked up by DB poller",
                    task.getTaskId());
            return;
        }
        try {
            rabbitTemplate.convertAndSend(queueName, task.getTaskId());
            LOG.debug("Enqueued integration task {} to queue {}", task.getTaskId(), queueName);
        } catch (Exception ex) {
            // Non-fatal: the scheduled PartnerIntegrationWorker will poll the DB
            LOG.warn("Failed to enqueue task {} to RabbitMQ (will retry via DB poll): {}",
                    task.getTaskId(), ex.getMessage());
        }
    }
}
