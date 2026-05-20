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
public class PaymentSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentSchemaMigration.class);
    private static final int ENUM_COLUMN_LENGTH = 32;

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public PaymentSchemaMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!isMysqlFamily()) {
                return;
            }

            migrateEnumColumn("payment_order", "provider");
            migrateEnumColumn("payment_order", "status");
            migrateEnumColumn("payment_transaction", "status");
        } catch (DataAccessException ex) {
            log.warn("Could not auto-migrate payment enum columns: {}", ex.getMessage());
        }
    }

    private void migrateEnumColumn(String tableName, String columnName) {
        try {
            Map<String, Object> column = jdbcTemplate.queryForMap("""
                    SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND COLUMN_NAME = ?
                    """, tableName, columnName);

            String dataType = String.valueOf(column.get("DATA_TYPE"));
            Integer length = null;
            Object rawLength = column.get("CHARACTER_MAXIMUM_LENGTH");
            if (rawLength instanceof Number number) {
                length = number.intValue();
            }

            if ("enum".equalsIgnoreCase(dataType)
                    || ("varchar".equalsIgnoreCase(dataType) && length != null && length < ENUM_COLUMN_LENGTH)) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " VARCHAR(" + ENUM_COLUMN_LENGTH + ") NOT NULL");
                log.info("Updated {}.{} column to VARCHAR({}) for future-safe enum storage", tableName, columnName, ENUM_COLUMN_LENGTH);
            }
        } catch (EmptyResultDataAccessException ex) {
            log.debug("{}.{} column not found yet; skipping payment schema migration", tableName, columnName);
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
            log.debug("Could not detect database product for payment schema migration", ex);
            return false;
        }
    }
}
