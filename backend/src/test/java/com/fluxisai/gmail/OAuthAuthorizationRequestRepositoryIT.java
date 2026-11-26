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
class OAuthAuthorizationRequestRepositoryIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private OAuthAuthorizationRequestRepository requests;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void catalogContainsExpectedConstraintsAndIndexes() {
        List<String> constraintNames = jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'oauth_authorization_requests'::regclass",
                String.class);
        assertThat(constraintNames).contains(
                "pk_oauth_authorization_requests",
                "fk_oauth_authorization_requests_user",
                "ux_oauth_authorization_requests_state_hash",
                "ck_oauth_authorization_requests_provider",
                "ck_oauth_authorization_requests_state_hash_format",
                "ck_oauth_authorization_requests_key_version",
                "ck_oauth_authorization_requests_expiry");

        String deleteAction = jdbcTemplate.queryForObject(
                "SELECT confdeltype FROM pg_constraint WHERE conname = 'fk_oauth_authorization_requests_user'",
                String.class);
        assertThat(deleteAction).isEqualTo("c");

        String expiryDefinition = jdbcTemplate.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                        + "WHERE conname = 'ck_oauth_authorization_requests_expiry'",
                String.class);
        assertThat(expiryDefinition).contains("expires_at > created_at");

        List<String> indexNames = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'oauth_authorization_requests'",
                String.class);
        assertThat(indexNames).contains(
                "ux_oauth_authorization_requests_state_hash",
                "ix_oauth_authorization_requests_user_id",
                "ix_oauth_authorization_requests_expires_at");
    }

    @Test
    void storesAndReadsRequestedScopesInOrderWithImmutableContract() {
        User user = saveUser();
        List<String> scopes = List.of("scope-z", "scope-a", "scope-m");
        OAuthAuthorizationRequest saved = requests.saveAndFlush(newRequest(
                user.getId(),
                stateHash(),
                scopes,
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)));

        entityManager.clear();
        OAuthAuthorizationRequest reloaded = requests.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getRequestedScopes()).containsExactlyElementsOf(scopes);
        assertThatThrownBy(() -> reloaded.getRequestedScopes().add("scope-extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void storesEmptyScopesAsEmptyPostgresArrayAndReadsEmptyList() {
        User user = saveUser();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OAuthAuthorizationRequest saved = requests.saveAndFlush(newRequest(
                user.getId(), stateHash(), List.of(), now, now.plusHours(1)));

        String rawScopes = jdbcTemplate.queryForObject(
                "SELECT requested_scopes::text FROM oauth_authorization_requests WHERE id = ?",
                String.class,
                saved.getId());
        entityManager.clear();
        OAuthAuthorizationRequest reloaded = requests.findById(saved.getId()).orElseThrow();

        assertThat(rawScopes).isEqualTo("{}");
        assertThat(reloaded.getRequestedScopes()).isEmpty();
    }

    @Test
    void enforcesUniqueStateHash() {
        User user = saveUser();
        String stateHash = stateHash();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        requests.saveAndFlush(newRequest(user.getId(), stateHash, List.of("scope-a"), now, now.plusHours(1)));

        assertThatThrownBy(() -> requests.saveAndFlush(newRequest(
                user.getId(), stateHash, List.of("scope-b"), now, now.plusHours(1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cascadesOnUserDeletion() {
        User user = saveUser();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        requests.saveAndFlush(newRequest(
                user.getId(), stateHash(), List.of("scope-a"), now, now.plusHours(1)));

        users.deleteById(user.getId());
        users.flush();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM oauth_authorization_requests WHERE user_id = ?",
                Integer.class,
                user.getId());
        assertThat(count).isZero();
    }

    @Test
    void rejectsInvalidExpiryRange() {
        User user = saveUser();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        assertThatThrownBy(() -> requests.saveAndFlush(newRequest(
                user.getId(), stateHash(), List.of("scope-a"), now, now.minusSeconds(1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void defaultsConsumedAtToNull() {
        User user = saveUser();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OAuthAuthorizationRequest saved = requests.saveAndFlush(newRequest(
                user.getId(), stateHash(), List.of("scope-a"), now, now.plusHours(1)));

        entityManager.clear();
        OAuthAuthorizationRequest reloaded = requests.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getConsumedAt()).isNull();
    }

    @Test
    void toStringDoesNotExposeStateHashOrEncryptedVerifier() {
        OAuthAuthorizationRequest request = new OAuthAuthorizationRequest(
                UUID.randomUUID(),
                ConnectedAccountProvider.GOOGLE,
                "STATE-HASH-SECRET-123",
                "ENCRYPTED-VERIFIER-SECRET-456",
                1,
                List.of("scope-a"),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));

        assertThat(request.toString())
                .doesNotContain("STATE-HASH-SECRET-123", "ENCRYPTED-VERIFIER-SECRET-456")
                .contains("OAuthAuthorizationRequest", "expiresAt=");
    }

    private OAuthAuthorizationRequest newRequest(
            UUID userId,
            String stateHash,
            List<String> scopes,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {
        return new OAuthAuthorizationRequest(
                userId,
                ConnectedAccountProvider.GOOGLE,
                stateHash,
                "v1:encrypted-code-verifier",
                1,
                scopes,
                createdAt,
                expiresAt);
    }

    private String stateHash() {
        return UUID.randomUUID().toString().replace("-", "").repeat(2);
    }

    private User saveUser() {
        return users.saveAndFlush(new User(
                "oauth-request-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic OAuth Request User",
                new BCryptPasswordEncoder().encode("synthetic-test-password")));
    }
}