package com.acme.foundation.domain.seed;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable representation of local sample dataset seeding state.
 */
public final class SampleDatasetState {

    public enum SeedStatus {
        NOT_STARTED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    private final String datasetName;
    private final String datasetVersion;
    private final SeedStatus seedStatus;
    private final Map<String, Long> recordCounts;
    private final Instant seededAt;

    public SampleDatasetState(
            String datasetName,
            String datasetVersion,
            SeedStatus seedStatus,
            Map<String, Long> recordCounts,
            Instant seededAt) {
        this.datasetName = datasetName;
        this.datasetVersion = datasetVersion;
        this.seedStatus = seedStatus;
        this.recordCounts = recordCounts != null
                ? Collections.unmodifiableMap(recordCounts)
                : Collections.emptyMap();
        this.seededAt = seededAt;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public SeedStatus getSeedStatus() {
        return seedStatus;
    }

    public Map<String, Long> getRecordCounts() {
        return recordCounts;
    }

    public Instant getSeededAt() {
        return seededAt;
    }
}
