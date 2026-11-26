-- Allow accounts to require a new Google authorization without deleting
-- the previous account identity metadata.
ALTER TABLE connected_accounts
    DROP CONSTRAINT connected_accounts_status_check;

ALTER TABLE connected_accounts
    ADD CONSTRAINT ck_connected_accounts_status
    CHECK (status IN ('CONNECTED', 'REAUTH_REQUIRED', 'REVOKED'));

-- REAUTH_REQUIRED and CONNECTED must not have a revocation timestamp.
ALTER TABLE connected_accounts
    DROP CONSTRAINT ck_connected_accounts_revoked_at;

ALTER TABLE connected_accounts
    ADD CONSTRAINT ck_connected_accounts_revoked_at
    CHECK (
        (status = 'CONNECTED' AND revoked_at IS NULL)
        OR (status = 'REAUTH_REQUIRED' AND revoked_at IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
    );

-- A reauthorization request must not retain invalid token ciphertext.
ALTER TABLE connected_accounts
    ADD CONSTRAINT ck_connected_accounts_reauth_tokens_cleared
    CHECK (
        status <> 'REAUTH_REQUIRED'
        OR (
            access_token_encrypted IS NULL
            AND refresh_token_encrypted IS NULL
        )
    );