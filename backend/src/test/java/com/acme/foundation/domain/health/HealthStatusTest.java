package com.acme.foundation.domain.health;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthStatusTest {

    @Test
    void constructor_setsStatusAndCheckedAt() {
        HealthStatus status = new HealthStatus(HealthStatus.Status.UP, Map.of());

        assertThat(status.getStatus()).isEqualTo(HealthStatus.Status.UP);
        assertThat(status.getCheckedAt()).isNotNull();
    }

    @Test
    void constructor_throwsOnNullStatus() {
        assertThatThrownBy(() -> new HealthStatus(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deriveOverallStatus_returnsDown_whenAnyComponentIsDown() {
        HealthStatus.ComponentHealth up = new HealthStatus.ComponentHealth(
                HealthStatus.ComponentHealth.Status.UP, Map.of());
        HealthStatus.ComponentHealth down = new HealthStatus.ComponentHealth(
                HealthStatus.ComponentHealth.Status.DOWN, Map.of());

        HealthStatus status = new HealthStatus(HealthStatus.Status.UP,
                Map.of("db", down, "app", up));

        assertThat(status.deriveOverallStatus()).isEqualTo(HealthStatus.Status.DOWN);
    }

    @Test
    void deriveOverallStatus_returnsUp_whenAllComponentsAreUp() {
        HealthStatus.ComponentHealth up = new HealthStatus.ComponentHealth(
                HealthStatus.ComponentHealth.Status.UP, Map.of());

        HealthStatus status = new HealthStatus(HealthStatus.Status.UP, Map.of("db", up));

        assertThat(status.deriveOverallStatus()).isEqualTo(HealthStatus.Status.UP);
    }

    @Test
    void components_areImmutable() {
        var mutableMap = new java.util.HashMap<String, HealthStatus.ComponentHealth>();
        HealthStatus status = new HealthStatus(HealthStatus.Status.UP, mutableMap);

        assertThatThrownBy(() -> status.getComponents().put("x",
                new HealthStatus.ComponentHealth(HealthStatus.ComponentHealth.Status.UP, Map.of())))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
