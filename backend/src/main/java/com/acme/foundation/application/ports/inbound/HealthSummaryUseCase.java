package com.acme.foundation.application.ports.inbound;

import com.acme.foundation.domain.health.HealthStatus;

/**
 * Inbound port: retrieve a health summary for the service.
 */
public interface HealthSummaryUseCase {

    /**
     * Returns the current health status including component breakdown.
     */
    HealthStatus getHealthSummary();
}
