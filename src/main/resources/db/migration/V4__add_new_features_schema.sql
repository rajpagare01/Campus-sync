CREATE TABLE IF NOT EXISTS society_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    society_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(1024),
    rejection_reason VARCHAR(1024),
    requested_at DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6),
    reviewed_by BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_sm_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX idx_sm_society_user ON society_memberships(society_id, user_id);
CREATE INDEX idx_sm_society_status ON society_memberships(society_id, status);
CREATE INDEX idx_sm_user ON society_memberships(user_id);

CREATE TABLE IF NOT EXISTS event_registration_fields (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    required BOOLEAN NOT NULL DEFAULT FALSE,
    options TEXT,
    placeholder VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_erf_event FOREIGN KEY (event_id) REFERENCES event (id)
);

CREATE INDEX idx_erf_event ON event_registration_fields(event_id);
CREATE INDEX idx_erf_event_order ON event_registration_fields(event_id, display_order);

CREATE TABLE IF NOT EXISTS event_registration_answers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    label_snapshot VARCHAR(255) NOT NULL,
    answer_value TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_era_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_era_field FOREIGN KEY (field_id) REFERENCES event_registration_fields (id)
);

CREATE INDEX idx_era_registration ON event_registration_answers(registration_id);
CREATE INDEX idx_era_field ON event_registration_answers(field_id);
CREATE UNIQUE INDEX idx_era_reg_field ON event_registration_answers(registration_id, field_id);

-- Alter existing tables
SET @event_certificate_enabled = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE event ADD COLUMN certificate_enabled BOOLEAN NOT NULL DEFAULT FALSE',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'event'
      AND column_name = 'certificate_enabled'
);
PREPARE stmt1 FROM @event_certificate_enabled;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @reg_certificate_downloaded_at = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE registration ADD COLUMN certificate_downloaded_at DATETIME(6)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'registration'
      AND column_name = 'certificate_downloaded_at'
);
PREPARE stmt2 FROM @reg_certificate_downloaded_at;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
