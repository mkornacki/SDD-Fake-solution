package com.acme.foundation.domain.health;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Machine-readable representation of service liveness and readiness state.
 */
public final class HealthStatus {

    public enum Status { UP, DOWN, OUT_OF_SERVICE }

    private final Status status;
    private final Map<String, ComponentHealth> components;
    private final Instant checkedAt;

    public HealthStatus(Status status, Map<String, ComponentHealth> components) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
        this.components = components != null
                ? Collections.unmodifiableMap(components)
                : Collections.emptyMap();
        this.checkedAt = Instant.now();
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, ComponentHealth> getComponents() {
        return components;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    /**
     * Returns DOWN if any component is DOWN; otherwise returns this status.
     */
    public Status deriveOverallStatus() {
        boolean anyDown = components.values().stream()
                .anyMatch(c -> c.getStatus() == ComponentHealth.Status.DOWN);
        return anyDown ? Status.DOWN : status;
    }

    public static final class ComponentHealth {
        public enum Status { UP, DOWN }

        private final Status status;
        private final Map<String, String> details;

        public ComponentHealth(Status status, Map<String, String> details) {
            this.status = status;
            this.details = details != null
                    ? Collections.unmodifiableMap(details)
                    : Collections.emptyMap();
        }

        public Status getStatus() {
            return status;
        }

        public Map<String, String> getDetails() {
            return details;
        }
    }
}
