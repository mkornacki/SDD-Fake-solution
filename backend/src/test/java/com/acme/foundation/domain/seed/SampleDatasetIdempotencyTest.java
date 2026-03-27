package com.acme.foundation.domain.seed;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SampleDatasetIdempotencyTest {

    @Test
    void stateKeepsStableBusinessKeysAcrossRepeatedReads() {
        SampleDatasetState first = new SampleDatasetState(
                "foundation-sample-dataset",
                "1.0.0",
                SampleDatasetState.SeedStatus.COMPLETED,
                Map.of("reservations", 1L, "roomReservationItems", 1L),
                Instant.parse("2026-03-26T00:00:00Z"));

        SampleDatasetState second = new SampleDatasetState(
                "foundation-sample-dataset",
                "1.0.0",
                SampleDatasetState.SeedStatus.COMPLETED,
                Map.of("reservations", 1L, "roomReservationItems", 1L),
                Instant.parse("2026-03-26T00:00:00Z"));

        assertThat(first.getDatasetName()).isEqualTo(second.getDatasetName());
        assertThat(first.getDatasetVersion()).isEqualTo(second.getDatasetVersion());
        assertThat(first.getRecordCounts()).isEqualTo(second.getRecordCounts());
    }
}
