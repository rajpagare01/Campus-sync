SET @registration_payment_required = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN payment_required BOOLEAN DEFAULT FALSE',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'payment_required'
);
PREPARE stmt FROM @registration_payment_required;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @registration_payment_status = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN payment_status VARCHAR(32) NOT NULL DEFAULT ''NOT_REQUIRED''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'payment_status'
);
PREPARE stmt FROM @registration_payment_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @registration_payment_order_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN payment_order_id BIGINT',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'payment_order_id'
);
PREPARE stmt FROM @registration_payment_order_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @registration_attended = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN attended BOOLEAN DEFAULT FALSE',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'attended'
);
PREPARE stmt FROM @registration_attended;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @registration_attendance_status = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN attendance_status VARCHAR(32) NOT NULL DEFAULT ''NOT_CHECKED_IN''',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'attendance_status'
);
PREPARE stmt FROM @registration_attendance_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @registration_checked_in_at = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN checked_in_at DATETIME(6)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'checked_in_at'
);
PREPARE stmt FROM @registration_checked_in_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @registration_qr_code = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN qr_code VARCHAR(1024)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'qr_code'
);
PREPARE stmt FROM @registration_qr_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
