package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Scheduled cleanup job for purging expired idempotency records.
 */
@Component
public class IdempotencyRecordCleanupJob {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyRecordCleanupJob.class);

    private final IdempotencyRepository idempotencyRepository;
    private final Clock clock;

    @Autowired
    public IdempotencyRecordCleanupJob(IdempotencyRepository idempotencyRepository) {
        this(idempotencyRepository, Clock.systemUTC());
    }

    IdempotencyRecordCleanupJob(IdempotencyRepository idempotencyRepository, Clock clock) {
        this.idempotencyRepository = idempotencyRepository;
        this.clock = clock;
    }

    /**
     * Purges expired idempotency records on a fixed delay.
     */
    @Scheduled(fixedDelayString = "${reservation.idempotency.cleanup-interval-ms:3600000}")
    public void purgeExpiredRecords() {
        Instant cutoff = Instant.now(clock);
        long deleted = idempotencyRepository.deleteExpiredBefore(cutoff);
        if (deleted > 0) {
            LOG.info("Purged {} expired idempotency records before {}", deleted, cutoff);
        }
    }
}