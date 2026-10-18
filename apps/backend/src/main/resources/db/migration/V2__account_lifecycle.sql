ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
-- Existing accounts must verify email; old sessions must not bypass verification.
DELETE FROM login_sessions;
CREATE TABLE account_tokens (
 token_hash VARCHAR(64) NOT NULL PRIMARY KEY,
 user_id BIGINT NOT NULL,
 purpose ENUM('VERIFY_EMAIL','RESET_PASSWORD') NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_account_token_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT uk_account_token_purpose UNIQUE(user_id,purpose)
);
CREATE INDEX idx_account_token_expiry ON account_tokens(expires_at);
