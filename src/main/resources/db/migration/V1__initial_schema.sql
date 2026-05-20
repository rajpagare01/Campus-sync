CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_code VARCHAR(255),
    role VARCHAR(32),
    token_version INT DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at DATETIME(6),
    deactivation_reason VARCHAR(255),
    bio VARCHAR(1000),
    profile_picture_url VARCHAR(500),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    venue VARCHAR(255),
    date DATETIME(6),
    type VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    price DOUBLE,
    image_url VARCHAR(500),
    views_count BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at DATETIME(6),
    CONSTRAINT fk_event_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS registration (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    event_id BIGINT,
    status VARCHAR(32) NOT NULL,
    payment_required BOOLEAN DEFAULT FALSE,
    payment_status VARCHAR(32) NOT NULL,
    payment_order_id BIGINT,
    attended BOOLEAN DEFAULT FALSE,
    attendance_status VARCHAR(32) NOT NULL,
    checked_in_at DATETIME(6),
    qr_code VARCHAR(1024),
    created_at DATETIME(6),
    CONSTRAINT fk_registration_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES event(id)
);

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    amount_in_minor_units BIGINT,
    checkout_public_key VARCHAR(255),
    client_request_id VARCHAR(128),
    created_at DATETIME(6),
    currency VARCHAR(32),
    event_id BIGINT NOT NULL,
    failure_reason VARCHAR(1000),
    paid_at DATETIME(6),
    provider VARCHAR(32) NOT NULL,
    provider_order_id VARCHAR(255),
    provider_payment_id VARCHAR(255),
    refunded_at DATETIME(6),
    registration_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    updated_at DATETIME(6),
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_payment_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_payment_order_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_payment_order_registration FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT uk_payment_order_registration UNIQUE (registration_id)
);

SET @payment_order_client_request_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE payment_order ADD COLUMN client_request_id VARCHAR(128)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_order'
      AND column_name = 'client_request_id'
);
PREPARE stmt FROM @payment_order_client_request_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payment_order_checkout_public_key = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE payment_order ADD COLUMN checkout_public_key VARCHAR(255)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_order'
      AND column_name = 'checkout_public_key'
);
PREPARE stmt FROM @payment_order_checkout_public_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payment_order_failure_reason = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE payment_order ADD COLUMN failure_reason VARCHAR(1000)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_order'
      AND column_name = 'failure_reason'
);
PREPARE stmt FROM @payment_order_failure_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_payment_order_client_request = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_payment_order_client_request ON payment_order(client_request_id, user_id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_order'
      AND index_name = 'idx_payment_order_client_request'
);
PREPARE stmt FROM @idx_payment_order_client_request;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_payment_order_provider_order_id = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_payment_order_provider_order_id ON payment_order(provider_order_id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_order'
      AND index_name = 'idx_payment_order_provider_order_id'
);
PREPARE stmt FROM @idx_payment_order_provider_order_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    transaction_type VARCHAR(128),
    gateway_event_id VARCHAR(128),
    provider_reference VARCHAR(255),
    amount_in_minor_units BIGINT,
    currency VARCHAR(32),
    gateway_event_type VARCHAR(128),
    failure_reason VARCHAR(1000),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_payment_transaction_order FOREIGN KEY (order_id) REFERENCES payment_order(id)
);

SET @payment_transaction_gateway_event_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE payment_transaction ADD COLUMN gateway_event_id VARCHAR(128)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_transaction'
      AND column_name = 'gateway_event_id'
);
PREPARE stmt FROM @payment_transaction_gateway_event_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payment_transaction_gateway_event_type = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE payment_transaction ADD COLUMN gateway_event_type VARCHAR(128)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_transaction'
      AND column_name = 'gateway_event_type'
);
PREPARE stmt FROM @payment_transaction_gateway_event_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payment_transaction_failure_reason = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE payment_transaction ADD COLUMN failure_reason VARCHAR(1000)',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_transaction'
      AND column_name = 'failure_reason'
);
PREPARE stmt FROM @payment_transaction_failure_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uk_payment_transaction_gateway_event = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE UNIQUE INDEX uk_payment_transaction_gateway_event ON payment_transaction(gateway_event_id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_transaction'
      AND index_name = 'uk_payment_transaction_gateway_event'
);
PREPARE stmt FROM @uk_payment_transaction_gateway_event;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(2000) NOT NULL,
    media_url VARCHAR(500),
    author_id BIGINT NOT NULL,
    event_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_posts_event FOREIGN KEY (event_id) REFERENCES event(id)
);

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(1000) NOT NULL,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id)
);

CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE TABLE IF NOT EXISTS follow_relationships (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES users(id),
    CONSTRAINT fk_follow_following FOREIGN KEY (following_id) REFERENCES users(id),
    CONSTRAINT uk_follow_pair UNIQUE (follower_id, following_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    related_id BIGINT,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_feedback_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT uk_feedback_user_event UNIQUE (user_id, event_id)
);

CREATE TABLE IF NOT EXISTS report (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(32),
    target_id BIGINT,
    reason VARCHAR(255),
    details VARCHAR(1000),
    status VARCHAR(32),
    reporter_id BIGINT NOT NULL,
    reviewer_id BIGINT,
    resolution_notes VARCHAR(1000),
    created_at DATETIME(6),
    reviewed_at DATETIME(6),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_report_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at DATETIME(6),
    CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id),
    CONSTRAINT fk_ai_message_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_log_entry (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(255),
    entity_type VARCHAR(255),
    entity_id BIGINT,
    details VARCHAR(1000),
    created_at DATETIME(6),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);
