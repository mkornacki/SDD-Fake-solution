package com.acme.reservation;

import com.acme.reservation.adapters.outbound.persistence.IdempotencyRecordCleanupJob;
import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "reservation.idempotency.cleanup-interval-ms=6000000"
})
class IdempotencyRecordCleanupIT {

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private IdempotencyRecordCleanupJob cleanupJob;

    @Test
    void cleanupJob_purgesOnlyExpiredRecords() {
        Instant now = Instant.now();
        String expiredKey = "expired-idempotency-key";
        String activeKey = "active-idempotency-key";

        IdempotencyRecord expired = new IdempotencyRecord(
                expiredKey,
                IdempotencyRecord.OperationType.CREATE,
                now.minus(40, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS));
        IdempotencyRecord active = new IdempotencyRecord(
                activeKey,
                IdempotencyRecord.OperationType.CREATE,
                now.minus(1, ChronoUnit.DAYS),
                now.plus(29, ChronoUnit.DAYS));

        idempotencyRepository.save(expired);
        idempotencyRepository.save(active);

        cleanupJob.purgeExpiredRecords();

        assertThat(idempotencyRepository.findByKey(expiredKey)).isEmpty();
        assertThat(idempotencyRepository.findByKey(activeKey)).isPresent();
    }
}