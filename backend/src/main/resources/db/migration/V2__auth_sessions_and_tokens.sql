-- Root integrates these statements into versioned migration. Existing accounts must verify email in prod.
ALTER TABLE users ADD COLUMN credentials_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE auth_refresh_sessions (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 token_hash VARCHAR(64) NOT NULL,
 family_id VARCHAR(36) NOT NULL,
 user_id BIGINT NOT NULL,
 credentials_version BIGINT NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 revoked_at TIMESTAMP(6) NULL,
 CONSTRAINT uq_auth_refresh_hash UNIQUE (token_hash),
 CONSTRAINT fk_auth_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX ix_auth_session_family (family_id), INDEX ix_auth_session_user (user_id)
);
CREATE TABLE auth_action_tokens (
 token_hash VARCHAR(64) NOT NULL PRIMARY KEY,
 user_id BIGINT NOT NULL,
 purpose VARCHAR(32) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 consumed_at TIMESTAMP(6) NULL,
 CONSTRAINT fk_auth_action_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX ix_auth_action_user (user_id,purpose)
);
