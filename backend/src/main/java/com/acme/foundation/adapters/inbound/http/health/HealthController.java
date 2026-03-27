package com.acme.foundation.adapters.inbound.http.health;

import com.acme.foundation.adapters.inbound.http.model.ApiResponse;
import com.acme.foundation.adapters.inbound.http.model.ResponseMeta;
import com.acme.foundation.application.ports.inbound.HealthSummaryUseCase;
import com.acme.foundation.domain.health.HealthStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authenticated health summary endpoint.
 * GET /api/v1/health — Returns JSON ApiResponse with health status.
 * Requires valid JWT bearer token.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final HealthSummaryUseCase healthSummaryUseCase;

    public HealthController(HealthSummaryUseCase healthSummaryUseCase) {
        this.healthSummaryUseCase = healthSummaryUseCase;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        HealthStatus summary = healthSummaryUseCase.getHealthSummary();

        Map<String, Object> data = Map.of(
                "status", summary.getStatus().name(),
                "checkedAt", summary.getCheckedAt().toString()
        );

        return ResponseEntity.ok(ApiResponse.of(data, ResponseMeta.defaults()));
    }
}
