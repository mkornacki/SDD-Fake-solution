package com.acme.foundation.seed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        "spring.datasource.url=jdbc:sqlite:target/database-seed-it.db",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/acme",
        "spring.task.scheduling.enabled=false",
        "server.port=0",
        "management.server.port=0"
})
@ActiveProfiles("local")
class DatabaseSeedIT {

    private static final Path DATABASE_PATH = Paths.get("target/database-seed-it.db");

    static {
        try {
            Files.deleteIfExists(DATABASE_PATH);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to prepare DatabaseSeedIT SQLite file", ex);
        }
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void startupAppliesMigrationsAndSeedsBaselineData() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertThat(singleLong(statement, "SELECT COUNT(*) FROM DATABASECHANGELOG")).isGreaterThan(0L);
            assertThat(singleLong(statement,
                    "SELECT COUNT(*) FROM reservations WHERE reservation_id = 'res-dev-001'"))
                    .isEqualTo(1L);
            assertThat(singleLong(statement,
                    "SELECT COUNT(*) FROM room_reservation_items WHERE room_item_id = 'room-dev-001'"))
                    .isEqualTo(1L);
            assertThat(singleString(statement,
                    "SELECT seed_status FROM sample_dataset_metadata WHERE dataset_name = 'foundation-sample-dataset'"))
                    .isEqualTo("COMPLETED");
        }
    }

    private long singleLong(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private String singleString(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}