package com.acme.foundation.adapters.outbound.persistence;

import com.acme.foundation.domain.seed.SampleDatasetState;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads seed metadata and deterministic sample-data counts from the local database.
 */
@Component
public class SampleDataStatusRepositoryAdapter {

    private final DataSource dataSource;

    public SampleDataStatusRepositoryAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SampleDatasetState readCurrentState(String datasetName, String datasetVersion) {
        Map<String, Long> recordCounts = readRecordCounts();
        SampleDatasetState.SeedStatus seedStatus = readSeedStatus(datasetName);
        Instant seededAt = readSeededAt(datasetName);
        return new SampleDatasetState(datasetName, datasetVersion, seedStatus, recordCounts, seededAt);
    }

    private Map<String, Long> readRecordCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("reservations", safeCount("SELECT COUNT(*) FROM reservations"));
        counts.put("roomReservationItems", safeCount("SELECT COUNT(*) FROM room_reservation_items"));
        return counts;
    }

    private long safeCount(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private SampleDatasetState.SeedStatus readSeedStatus(String datasetName) {
        String sql = "SELECT seed_status FROM sample_dataset_metadata WHERE dataset_name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, datasetName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return SampleDatasetState.SeedStatus.valueOf(rs.getString("seed_status"));
                }
            }
        } catch (Exception ignored) {
            // Fall back below when metadata table is not present in non-local profiles.
        }
        return SampleDatasetState.SeedStatus.NOT_STARTED;
    }

    private Instant readSeededAt(String datasetName) {
        String sql = "SELECT seeded_at FROM sample_dataset_metadata WHERE dataset_name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, datasetName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("seeded_at");
                    return value != null ? Instant.parse(value) : null;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
