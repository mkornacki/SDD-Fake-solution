package com.acme.foundation.adapters.inbound.http.dev;

import com.acme.foundation.application.SampleDataStatusService;
import com.acme.foundation.domain.seed.SampleDatasetState;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authenticated endpoint for validating local sample-data bootstrap state.
 */
@RestController
@RequestMapping("/api/v1/dev/sample-data")
public class SampleDataStatusController {

    private final SampleDataStatusService sampleDataStatusService;

    public SampleDataStatusController(SampleDataStatusService sampleDataStatusService) {
        this.sampleDataStatusService = sampleDataStatusService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatus() {
        SampleDatasetState state = sampleDataStatusService.getStatus();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("datasetName", state.getDatasetName());
        payload.put("datasetVersion", state.getDatasetVersion());
        payload.put("seedStatus", state.getSeedStatus().name());
        payload.put("recordCounts", state.getRecordCounts());
        payload.put("seededAt", state.getSeededAt() != null ? state.getSeededAt().toString() : null);
        return ResponseEntity.ok(payload);
    }
}
