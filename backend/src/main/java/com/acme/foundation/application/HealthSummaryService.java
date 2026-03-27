package com.acme.foundation.application;

import com.acme.foundation.application.ports.inbound.HealthSummaryUseCase;
import com.acme.foundation.domain.health.HealthStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application service implementing HealthSummaryUseCase.
 * Delegates component health checks to Spring Boot Actuator's HealthEndpoint.
 */
@Service
public class HealthSummaryService implements HealthSummaryUseCase {

    private final HealthEndpoint healthEndpoint;

    public HealthSummaryService(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @Override
    public HealthStatus getHealthSummary() {
        HealthComponent health = healthEndpoint.health();
        HealthStatus.Status status = mapStatus(health.getStatus().getCode());
        return new HealthStatus(status, mapComponents(health));
    }

    private Map<String, HealthStatus.ComponentHealth> mapComponents(HealthComponent health) {
        if (!(health instanceof SystemHealth)) {
            return Map.of();
        }

        Map<String, HealthStatus.ComponentHealth> components = new LinkedHashMap<>();
        SystemHealth systemHealth = (SystemHealth) health;
        for (Map.Entry<String, HealthComponent> entry : systemHealth.getComponents().entrySet()) {
            HealthComponent component = entry.getValue();
            Map<String, String> details = new LinkedHashMap<>();
            if (component instanceof Health) {
                ((Health) component).getDetails().forEach((key, value) -> details.put(key, String.valueOf(value)));
            }
            components.put(
                    entry.getKey(),
                    new HealthStatus.ComponentHealth(mapComponentStatus(component.getStatus().getCode()), details));
        }
        return components;
    }

    private HealthStatus.ComponentHealth.Status mapComponentStatus(String code) {
        if ("UP".equals(code)) {
            return HealthStatus.ComponentHealth.Status.UP;
        }
        return HealthStatus.ComponentHealth.Status.DOWN;
    }

    private HealthStatus.Status mapStatus(String code) {
        switch (code) {
            case "UP":
                return HealthStatus.Status.UP;
            case "DOWN":
                return HealthStatus.Status.DOWN;
            default:
                return HealthStatus.Status.OUT_OF_SERVICE;
        }
    }
}
