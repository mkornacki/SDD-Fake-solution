package com.acme.foundation.adapters.outbound.persistence;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom health indicator for database connectivity.
 * Reports UP when a connection can be obtained; DOWN otherwise.
 */
@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("database", "SQLite connection valid");
                details.put("migrationsApplied", hasAppliedMigrations(connection));
                details.put("seedStatus", readSeedStatus(connection));
                return Health.up().withDetails(details).build();
            }
            return Health.down().withDetail("database", "Connection validation failed").build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }

    private boolean hasAppliedMigrations(Connection connection) {
        String sql = "SELECT COUNT(*) FROM DATABASECHANGELOG";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() && rs.getLong(1) > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private String readSeedStatus(Connection connection) {
        String sql = "SELECT seed_status FROM sample_dataset_metadata WHERE dataset_name = 'foundation-sample-dataset'";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("seed_status");
            }
        } catch (Exception ex) {
            return "NOT_STARTED";
        }
        return "NOT_STARTED";
    }
}
