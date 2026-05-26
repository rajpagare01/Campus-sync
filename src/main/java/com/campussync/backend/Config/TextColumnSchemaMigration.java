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
public class TextColumnSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TextColumnSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public TextColumnSchemaMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!isMysqlFamily()) {
                return;
            }

            migrateToTextColumn("event", "description");
            migrateToTextColumn("users", "bio");
        } catch (DataAccessException ex) {
            log.warn("Could not auto-migrate text columns: {}", ex.getMessage());
        }
    }

    private void migrateToTextColumn(String tableName, String columnName) {
        try {
            Map<String, Object> column = jdbcTemplate.queryForMap("""
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, tableName, columnName);

            String dataType = String.valueOf(column.get("DATA_TYPE"));

            if ("varchar".equalsIgnoreCase(dataType)) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " TEXT");
                log.info("Updated {}.{} column to TEXT to prevent data truncation", tableName, columnName);
            }
        } catch (EmptyResultDataAccessException ex) {
            log.debug("{}.{} column not found yet; skipping text schema migration", tableName, columnName);
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
            log.debug("Could not detect database product for text schema migration", ex);
            return false;
        }
    }
}
