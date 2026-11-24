CREATE TABLE connected_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GOOGLE')),
    provider_account_id VARCHAR(255) NOT NULL,
    account_email VARCHAR(320) NOT NULL,
    access_token_encrypted TEXT,
    refresh_token_encrypted TEXT,
    token_expires_at TIMESTAMPTZ,
    granted_scopes TEXT[] NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'CONNECTED'
        CHECK (status IN ('CONNECTED', 'REVOKED')),
    encryption_key_version INTEGER NOT NULL DEFAULT 1
        CHECK (encryption_key_version > 0),
    connected_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT ux_connected_accounts_user_provider UNIQUE (user_id, provider),
    CONSTRAINT ck_connected_accounts_tokens_when_connected
        CHECK (status <> 'CONNECTED' OR access_token_encrypted IS NOT NULL),
    CONSTRAINT ck_connected_accounts_revoked_at
        CHECK (
            (status = 'CONNECTED' AND revoked_at IS NULL)
            OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
        )
);

CREATE INDEX ix_connected_accounts_user_status
    ON connected_accounts (user_id, status);
