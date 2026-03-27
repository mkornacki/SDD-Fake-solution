package com.acme.foundation.e2e;

import com.acme.foundation.adapters.outbound.persistence.SampleDataSeeder;
import com.acme.foundation.application.SampleDataStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/dev-environment-bootstrap-e2e.db",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/acme",
        "spring.task.scheduling.enabled=false",
        "server.port=0",
        "management.server.port=0"
})
@ActiveProfiles("local")
class DevEnvironmentBootstrapE2ETest {

    private static final Path DATABASE_PATH = Paths.get("target/dev-environment-bootstrap-e2e.db");

    static {
        try {
            Files.deleteIfExists(DATABASE_PATH);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to prepare DevEnvironmentBootstrapE2ETest SQLite file", ex);
        }
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SampleDataSeeder sampleDataSeeder;

    @Autowired
    private SampleDataStatusService sampleDataStatusService;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Test
    @WithMockUser(authorities = "SCOPE_dev:sample-data")
    void bootstrapFlow_coversReadinessSeededDataAndBaselineRecovery() throws Exception {
        HealthComponent readiness = healthEndpoint.healthForPath("readiness");
        assertThat(readiness.getStatus().getCode()).isEqualTo("UP");

        assertThat(sampleDataStatusService.getStatus().getDatasetVersion()).isEqualTo("1.0.0");
        assertThat(sampleDataStatusService.getStatus().getRecordCounts()).containsEntry("reservations", 1L);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM room_reservation_items");
            statement.execute("DELETE FROM reservations");
        }

        sampleDataSeeder.run(new DefaultApplicationArguments(new String[0]));

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertThat(singleLong(statement,
                "SELECT COUNT(*) FROM reservations WHERE reservation_id = 'res-dev-001'"))
                .isEqualTo(1L);
            assertThat(singleLong(statement,
                "SELECT COUNT(*) FROM room_reservation_items WHERE room_item_id = 'room-dev-001'"))
                .isEqualTo(1L);
        }
        }

        private long singleLong(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}