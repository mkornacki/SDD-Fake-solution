package com.acme.foundation.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

/**
 * Local-profile startup validation to fail fast on invalid bootstrap configuration.
 */
@Configuration
@Profile("local")
public class StartupValidationConfig {

    private final Environment environment;

    public StartupValidationConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        if (datasourceUrl.trim().isEmpty()) {
            throw new IllegalStateException("Startup validation failed: spring.datasource.url must be configured.");
        }
        if (!datasourceUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalStateException(
                    "Startup validation failed: local profile requires a SQLite datasource (jdbc:sqlite:...).");
        }

        String issuer = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "");
        if (issuer.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Startup validation failed: spring.security.oauth2.resourceserver."
                            + "jwt.issuer-uri must be configured.");
        }
    }
}
