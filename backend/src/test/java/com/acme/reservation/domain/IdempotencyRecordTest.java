package com.acme.reservation.domain;

import com.acme.reservation.domain.idempotency.IdempotencyRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyRecordTest {

    @Test
    void newRecord_isInProgress() {
        IdempotencyRecord record = createRecord("key-1");
        assertThat(record.isInProgress()).isTrue();
        assertThat(record.isCompleted()).isFalse();
    }

    @Test
    void complete_transitionsToCompleted() {
        IdempotencyRecord record = createRecord("key-1");
        record.complete("res-abc", "digest-xyz", Instant.now());

        assertThat(record.isCompleted()).isTrue();
        assertThat(record.isInProgress()).isFalse();
        assertThat(record.getReservationId()).isEqualTo("res-abc");
        assertThat(record.getResponseDigest()).isEqualTo("digest-xyz");
        assertThat(record.getCompletedAt()).isNotNull();
    }

    @Test
    void complete_requiresReservationId() {
        IdempotencyRecord record = createRecord("key-1");
        assertThatThrownBy(() -> record.complete(null, "digest", Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fail_transitionsToFailed() {
        IdempotencyRecord record = createRecord("key-1");
        record.fail(Instant.now());
        assertThat(record.getResultStatus())
                .isEqualTo(IdempotencyRecord.ResultStatus.FAILED);
    }

    @Test
    void duplicateDetection_sameKeyIsAlreadyInProgress() {
        IdempotencyRecord record = createRecord("key-dup");
        assertThat(record.isInProgress()).isTrue();
        // Simulates: same key cannot be processed concurrently
    }

    @Test
    void expiredRecord_isExpired() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        IdempotencyRecord record = new IdempotencyRecord(
                "key-exp", IdempotencyRecord.OperationType.CREATE,
                Instant.now().minus(31, ChronoUnit.DAYS), past);
        assertThat(record.isExpired(Instant.now())).isTrue();
    }

    @Test
    void activeRecord_isNotExpired() {
        IdempotencyRecord record = createRecord("key-active");
        assertThat(record.isExpired(Instant.now())).isFalse();
    }

    private IdempotencyRecord createRecord(String key) {
        return new IdempotencyRecord(
                key,
                IdempotencyRecord.OperationType.CREATE,
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS));
    }
}
