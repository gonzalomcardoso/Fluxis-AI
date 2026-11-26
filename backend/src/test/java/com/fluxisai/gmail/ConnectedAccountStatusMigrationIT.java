package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConnectedAccountStatusMigrationIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void catalogContainsExpectedConnectedAccountConstraints() {
        List<String> constraintNames = jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'connected_accounts'::regclass",
                String.class);

        assertThat(constraintNames)
                .contains(
                        "ck_connected_accounts_status",
                        "ck_connected_accounts_revoked_at",
                        "ck_connected_accounts_reauth_tokens_cleared",
                        "ck_connected_accounts_tokens_when_connected")
                .doesNotContain("connected_accounts_status_check");

        String statusDefinition = constraintDefinition("ck_connected_accounts_status");
        assertThat(statusDefinition)
                .contains("CONNECTED", "REAUTH_REQUIRED", "REVOKED");

        String tokenDefinition = constraintDefinition("ck_connected_accounts_tokens_when_connected");
        assertThat(tokenDefinition)
                .contains("status", "CONNECTED", "access_token_encrypted IS NOT NULL")
                .doesNotContain("refresh_token_encrypted");

        String reauthTokensDefinition = constraintDefinition("ck_connected_accounts_reauth_tokens_cleared");
        assertThat(reauthTokensDefinition)
                .contains("REAUTH_REQUIRED", "access_token_encrypted IS NULL", "refresh_token_encrypted IS NULL");
    }

    @Test
    void acceptsReauthRequiredWithoutTokensOrRevocationTimestamp() {
        User user = saveUser();

        insertAccount(user.getId(), null, null, "REAUTH_REQUIRED", null);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM connected_accounts WHERE user_id = ?",
                String.class,
                user.getId());
        assertThat(status).isEqualTo("REAUTH_REQUIRED");
    }

    @Test
    void rejectsReauthRequiredWithAccessTokenCiphertext() {
        User user = saveUser();

        assertThatThrownBy(() -> insertAccount(
                user.getId(), "v1:access-token", null, "REAUTH_REQUIRED", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsReauthRequiredWithRefreshTokenCiphertext() {
        User user = saveUser();

        assertThatThrownBy(() -> insertAccount(
                user.getId(), null, "v1:refresh-token", "REAUTH_REQUIRED", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsReauthRequiredWithRevokedTimestamp() {
        User user = saveUser();

        assertThatThrownBy(() -> insertAccount(
                user.getId(), null, null, "REAUTH_REQUIRED", OffsetDateTime.now(ZoneOffset.UTC)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownConnectedAccountStatus() {
        User user = saveUser();

        assertThatThrownBy(() -> insertAccount(user.getId(), null, null, "INVALID_STATUS", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String constraintDefinition(String constraintName) {
        return jdbcTemplate.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?",
                String.class,
                constraintName);
    }

    private void insertAccount(
            UUID userId,
            String accessTokenEncrypted,
            String refreshTokenEncrypted,
            String status,
            OffsetDateTime revokedAt) {
        jdbcTemplate.update("""
                INSERT INTO connected_accounts (
                    id,
                    user_id,
                    provider,
                    provider_account_id,
                    account_email,
                    access_token_encrypted,
                    refresh_token_encrypted,
                    token_expires_at,
                    granted_scopes,
                    status,
                    encryption_key_version,
                    connected_at,
                    updated_at,
                    revoked_at
                ) VALUES (
                    ?, ?, 'GOOGLE', ?, ?, ?, ?, NULL,
                    ARRAY['scope-a']::text[], ?, 1, now(), now(), ?
                )
                """,
                UUID.randomUUID(),
                userId,
                "google-sub-" + UUID.randomUUID(),
                "person@example.test",
                accessTokenEncrypted,
                refreshTokenEncrypted,
                status,
                revokedAt);
    }

    private User saveUser() {
        return users.saveAndFlush(new User(
                "gmail-status-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic Gmail Status User",
                new BCryptPasswordEncoder().encode("synthetic-test-password")));
    }
}