-- Additive delivery/audit schema. Root incorporates this into versioned Flyway migrations.
CREATE TABLE notification_outbox (
 id VARCHAR(36) PRIMARY KEY,
 event_key VARCHAR(160) NOT NULL,
 kind VARCHAR(20) NOT NULL,
 payload LONGTEXT NOT NULL,
 status VARCHAR(16) NOT NULL,
 attempts INT NOT NULL,
 next_attempt_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 published_at DATETIME(6),
 last_error_code VARCHAR(80),
 CONSTRAINT uk_outbox_event_key UNIQUE(event_key),
 INDEX idx_outbox_dispatch(status, next_attempt_at)
);
CREATE TABLE notification_receipts (
 message_id VARCHAR(80) PRIMARY KEY,
 delivered_at DATETIME(6) NOT NULL
);
CREATE TABLE medical_audit_entries (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 actor_user_id BIGINT NOT NULL,
 action VARCHAR(32) NOT NULL,
 medical_record_id BIGINT,
 appointment_id BIGINT,
 occurred_at DATETIME(6) NOT NULL,
 INDEX idx_audit_record_time(medical_record_id, occurred_at)
);
CREATE TABLE medical_record_revisions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 medical_record_id BIGINT NOT NULL,
 actor_user_id BIGINT NOT NULL,
 before_diagnosis LONGTEXT,
 before_notes LONGTEXT,
 after_diagnosis LONGTEXT,
 after_notes LONGTEXT,
 occurred_at DATETIME(6) NOT NULL,
 INDEX idx_revisions_record(medical_record_id)
);
-- IDs intentionally retained without cascading foreign keys so deleting/deactivating an
-- account cannot erase audit evidence; clinical revisions must use restricted DB access.
