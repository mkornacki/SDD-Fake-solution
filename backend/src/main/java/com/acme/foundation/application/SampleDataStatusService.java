package com.acme.foundation.application;

import com.acme.foundation.adapters.outbound.persistence.SampleDataStatusRepositoryAdapter;
import com.acme.foundation.domain.seed.SampleDatasetState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Application service that returns the current deterministic sample-data state.
 */
@Service
public class SampleDataStatusService {

    private final SampleDataStatusRepositoryAdapter repository;
    private final String datasetName;
    private final String datasetVersion;

    public SampleDataStatusService(
            SampleDataStatusRepositoryAdapter repository,
            @Value("${sample-data.dataset-name:foundation-sample-dataset}") String datasetName,
            @Value("${sample-data.dataset-version:1.0.0}") String datasetVersion) {
        this.repository = repository;
        this.datasetName = datasetName;
        this.datasetVersion = datasetVersion;
    }

    public SampleDatasetState getStatus() {
        return repository.readCurrentState(datasetName, datasetVersion);
    }
}
