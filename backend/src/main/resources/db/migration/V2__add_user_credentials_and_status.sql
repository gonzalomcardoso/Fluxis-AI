ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DISABLED';

UPDATE users
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE password_hash IS NULL;

ALTER TABLE users
    ALTER COLUMN password_hash SET NOT NULL,
    ALTER COLUMN status DROP DEFAULT,
    ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED'));
