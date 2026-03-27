package com.acme.foundation.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Actuator configuration.
 * Liveness and readiness probes are configured via application.yml.
 * Health endpoints are excluded from authentication in SecurityConfig.
 */
@Configuration
public class ActuatorConfig {
    // Probe routing and exposure is fully managed via application.yml:
    //   management.endpoint.health.probes.enabled=true
    //   management.endpoints.web.exposure.include=health,info
    // No additional bean configuration required for basic liveness/readiness.
}
