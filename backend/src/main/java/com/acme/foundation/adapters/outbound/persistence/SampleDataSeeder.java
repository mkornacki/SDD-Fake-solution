package com.acme.foundation.adapters.outbound.persistence;

import com.acme.foundation.domain.seed.SampleDatasetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;

/**
 * Applies deterministic sample data after migrations for local profile startup.
 */
@Component
@Profile("local")
public class SampleDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDataSeeder.class);

    private final DataSource dataSource;

    public SampleDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        updateMetadata(SampleDatasetState.SeedStatus.RUNNING, null);

        try {
            String sql = StreamUtils.copyToString(
                    new ClassPathResource("db/seed/sample-data.sql").getInputStream(),
                    StandardCharsets.UTF_8);

            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                ensureBootstrapTables(statement);
                for (String chunk : sql.split(";")) {
                    String candidate = stripLineComments(chunk).trim();
                    if (!candidate.isEmpty()) {
                        statement.execute(candidate);
                    }
                }
            }

            updateMetadata(SampleDatasetState.SeedStatus.COMPLETED, Instant.now());
            LOG.info("Sample dataset seeding completed successfully.");
        } catch (Exception ex) {
            updateMetadata(SampleDatasetState.SeedStatus.FAILED, null);
            throw ex;
        }
    }

    private String stripLineComments(String sqlChunk) {
        StringBuilder builder = new StringBuilder();
        for (String line : sqlChunk.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private void updateMetadata(SampleDatasetState.SeedStatus status, Instant seededAt) throws Exception {
        String sql = "INSERT INTO sample_dataset_metadata("
            + "dataset_name, dataset_version, seed_status, seeded_at, updated_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) "
                + "ON CONFLICT(dataset_name) DO UPDATE SET "
                + "dataset_version = excluded.dataset_version, "
                + "seed_status = excluded.seed_status, "
                + "seeded_at = excluded.seeded_at, "
                + "updated_at = CURRENT_TIMESTAMP";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "foundation-sample-dataset");
            statement.setString(2, "1.0.0");
            statement.setString(3, status.name());
            statement.setString(4, seededAt != null ? seededAt.toString() : null);
            statement.executeUpdate();
        }
    }

            private void ensureBootstrapTables(Statement statement) throws Exception {
            statement.execute("CREATE TABLE IF NOT EXISTS reservations ("
                + "reservation_id TEXT NOT NULL PRIMARY KEY,"
                + "partner_id TEXT NOT NULL,"
                + "external_reference TEXT,"
                + "status TEXT NOT NULL,"
                + "currency_code TEXT NOT NULL,"
                + "total_price NUMERIC NOT NULL DEFAULT 0,"
                + "total_refund_amount NUMERIC NOT NULL DEFAULT 0,"
                + "total_cancellation_fee NUMERIC NOT NULL DEFAULT 0,"
                + "room_count INTEGER NOT NULL DEFAULT 0,"
                + "version INTEGER NOT NULL DEFAULT 0,"
                + "created_at TEXT NOT NULL,"
                + "updated_at TEXT NOT NULL,"
                + "guest_id TEXT NOT NULL)");

            statement.execute("CREATE TABLE IF NOT EXISTS room_reservation_items ("
                + "room_item_id TEXT NOT NULL PRIMARY KEY,"
                + "reservation_id TEXT NOT NULL,"
                + "room_code TEXT NOT NULL,"
                + "check_in_date TEXT NOT NULL,"
                + "check_out_date TEXT NOT NULL,"
                + "status TEXT NOT NULL,"
                + "base_price NUMERIC NOT NULL,"
                + "cancellation_fee NUMERIC,"
                + "refund_amount NUMERIC,"
                + "cancellation_reason TEXT,"
                + "cancelled_at TEXT,"
                + "processing_status TEXT NOT NULL DEFAULT 'NOT_STARTED',"
                + "FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id))");

            statement.execute("CREATE TABLE IF NOT EXISTS sample_dataset_metadata ("
                + "dataset_name TEXT NOT NULL PRIMARY KEY,"
                + "dataset_version TEXT NOT NULL,"
                + "seed_status TEXT NOT NULL,"
                + "seeded_at TEXT,"
                + "updated_at TEXT NOT NULL)");
            }
}
