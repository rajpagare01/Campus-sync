package com.campussync.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@Component
public class NotificationSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public NotificationSchemaMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!isMysqlFamily()) {
                return;
            }

            Map<String, Object> column = jdbcTemplate.queryForMap("""
                    SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, "notifications", "type");

            String dataType = String.valueOf(column.get("DATA_TYPE"));
            Integer length = null;
            Object rawLength = column.get("CHARACTER_MAXIMUM_LENGTH");
            if (rawLength instanceof Number number) {
                length = number.intValue();
            }

            if ("enum".equalsIgnoreCase(dataType)
                    || ("varchar".equalsIgnoreCase(dataType) && length != null && length < 32)) {
                jdbcTemplate.execute("ALTER TABLE notifications MODIFY COLUMN type VARCHAR(32) NOT NULL");
                log.info("Updated notifications.type column to VARCHAR(32) for future-safe enum storage");
            }
        } catch (EmptyResultDataAccessException ex) {
            log.debug("notifications.type column not found yet; skipping notification schema migration");
        } catch (DataAccessException ex) {
            log.warn("Could not auto-migrate notifications.type column: {}", ex.getMessage());
        }
    }

    private boolean isMysqlFamily() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            if (productName == null) {
                return false;
            }
            String normalized = productName.toLowerCase();
            return normalized.contains("mysql") || normalized.contains("mariadb");
        } catch (Exception ex) {
            log.debug("Could not detect database product for notification schema migration", ex);
            return false;
        }
    }
}
