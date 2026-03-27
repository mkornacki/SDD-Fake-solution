package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.application.ports.outbound.IdempotencyRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyRecordCleanupJobTest {

    @Test
    void purgeExpiredRecords_deletesUsingCurrentTimeCutoff() {
        IdempotencyRepository repository = mock(IdempotencyRepository.class);
        Instant fixedNow = Instant.parse("2026-03-26T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        when(repository.deleteExpiredBefore(fixedNow)).thenReturn(2L);

        IdempotencyRecordCleanupJob job = new IdempotencyRecordCleanupJob(repository, fixedClock);
        job.purgeExpiredRecords();

        verify(repository, times(1)).deleteExpiredBefore(eq(fixedNow));
    }
}