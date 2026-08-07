ALTER TABLE users
    ADD COLUMN password_reset_token_hash VARCHAR(64);

ALTER TABLE users
    ADD COLUMN password_reset_expires_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX users_password_reset_token_hash_uq
    ON users (password_reset_token_hash);

ALTER TABLE users DROP COLUMN recovery_code;
