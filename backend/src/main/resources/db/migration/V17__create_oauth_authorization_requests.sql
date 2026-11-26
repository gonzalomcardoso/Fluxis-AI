CREATE TABLE oauth_authorization_requests (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL,
    state_hash VARCHAR(64) NOT NULL,
    code_verifier_encrypted TEXT NOT NULL,
    encryption_key_version INTEGER NOT NULL,
    requested_scopes TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,

    CONSTRAINT pk_oauth_authorization_requests
        PRIMARY KEY (id),
    CONSTRAINT fk_oauth_authorization_requests_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ux_oauth_authorization_requests_state_hash
        UNIQUE (state_hash),
    CONSTRAINT ck_oauth_authorization_requests_provider
        CHECK (provider IN ('GOOGLE')),
    CONSTRAINT ck_oauth_authorization_requests_state_hash_format
        CHECK (state_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_oauth_authorization_requests_key_version
        CHECK (encryption_key_version > 0),
    CONSTRAINT ck_oauth_authorization_requests_expiry
        CHECK (expires_at > created_at)
);

-- The unique constraint already creates the state_hash index.
CREATE INDEX ix_oauth_authorization_requests_user_id
    ON oauth_authorization_requests (user_id);

CREATE INDEX ix_oauth_authorization_requests_expires_at
    ON oauth_authorization_requests (expires_at);