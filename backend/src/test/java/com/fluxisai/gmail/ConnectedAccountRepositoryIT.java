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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConnectedAccountRepositoryIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private ConnectedAccountRepository connectedAccounts;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void flywayAppliesConnectedAccountsMigration() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '15'", Integer.class);

        assertThat(applied).isEqualTo(1);
    }

    @Test
    void storesEncryptedTokenColumnsAndScopes() {
        User user = saveUser();
        ConnectedAccount account = connectedAccounts.saveAndFlush(new ConnectedAccount(
                user.getId(),
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                "v1:encrypted-access-token",
                "v1:encrypted-refresh-token",
                OffsetDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneOffset.UTC),
                GmailOAuthScopes.BASELINE,
                1));

        ConnectedAccount found = connectedAccounts
                .findByUserIdAndProvider(user.getId(), ConnectedAccountProvider.GOOGLE)
                .orElseThrow();
        String rawAccessToken = jdbcTemplate.queryForObject(
                "SELECT access_token_encrypted FROM connected_accounts WHERE id = ?",
                String.class,
                account.getId());

        assertThat(found.getProviderAccountId()).isEqualTo("google-sub-123");
        assertThat(found.getGrantedScopes()).containsExactlyElementsOf(GmailOAuthScopes.BASELINE);
        assertThat(found.getStatus()).isEqualTo(ConnectedAccountStatus.CONNECTED);
        assertThat(rawAccessToken).isEqualTo("v1:encrypted-access-token");
    }

    @Test
    void enforcesOneGoogleConnectionPerUser() {
        User user = saveUser();
        connectedAccounts.saveAndFlush(new ConnectedAccount(
                user.getId(),
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                "v1:first-access-token",
                null,
                null,
                List.of(GmailOAuthScopes.GMAIL_METADATA),
                1));

        assertThatThrownBy(() -> connectedAccounts.saveAndFlush(new ConnectedAccount(
                user.getId(),
                ConnectedAccountProvider.GOOGLE,
                "google-sub-456",
                "other@example.test",
                "v1:second-access-token",
                null,
                null,
                List.of(GmailOAuthScopes.GMAIL_METADATA),
                1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatesConnectedAtAndUpdatedAtWhenReconnectingExistingAccount() {
        User user = saveUser();
        ConnectedAccount account = connectedAccounts.saveAndFlush(new ConnectedAccount(
                user.getId(),
                ConnectedAccountProvider.GOOGLE,
                "google-sub-timestamps",
                "person@example.test",
                "v1:initial-access-token",
                "v1:initial-refresh-token",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1),
                GmailOAuthScopes.BASELINE,
                1));

        OffsetDateTime oldConnectedAt = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime oldUpdatedAt = OffsetDateTime.of(2020, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        jdbcTemplate.update(
                "UPDATE connected_accounts SET connected_at = ?, updated_at = ? WHERE id = ?",
                oldConnectedAt,
                oldUpdatedAt,
                account.getId());

        entityManager.clear();

        ConnectedAccount existing = connectedAccounts.findById(account.getId()).orElseThrow();
        existing.updateConnection(
                "person@example.test",
                "v1:new-access-token",
                "v1:new-refresh-token",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(2),
                GmailOAuthScopes.BASELINE,
                1);
        connectedAccounts.saveAndFlush(existing);

        entityManager.clear();

        ConnectedAccount reloaded = connectedAccounts.findById(account.getId()).orElseThrow();

        assertThat(reloaded.getConnectedAt()).isAfter(oldConnectedAt);
        assertThat(reloaded.getUpdatedAt()).isAfter(oldUpdatedAt);
        assertThat(reloaded.getAccessTokenEncrypted()).isEqualTo("v1:new-access-token");
    }

    private User saveUser() {
        return users.saveAndFlush(new User(
                "gmail-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic Gmail User",
                new BCryptPasswordEncoder().encode("synthetic-test-password")));
    }
}
