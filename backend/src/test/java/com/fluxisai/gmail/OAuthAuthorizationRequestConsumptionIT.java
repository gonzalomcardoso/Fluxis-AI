package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class OAuthAuthorizationRequestConsumptionIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private OAuthAuthorizationRequestRepository requests;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void markConsumedReturnsOneThenZeroAndPersistsTheConsumedTimestamp() {
        User user = saveUser();
        String stateHash = stateHash();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OAuthAuthorizationRequest request = saveRequest(
                user.getId(), stateHash, now.minusMinutes(5), now.plusHours(1));

        try {
            Integer first = inNewTransaction(() -> requests.markConsumed(stateHash, now));
            Integer second = inNewTransaction(() -> requests.markConsumed(stateHash, now));

            assertThat(first).isEqualTo(1);
            assertThat(second).isZero();
            assertThat(readConsumedAt(request.getId())).isNotNull();
        } finally {
            deleteUser(user);
        }
    }

    @Test
    void markConsumedReturnsZeroForExpiredRequest() {
        User user = saveUser();
        String stateHash = stateHash();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OAuthAuthorizationRequest request = saveRequest(
                user.getId(), stateHash, now.minusHours(2), now.minusHours(1));

        try {
            Integer result = inNewTransaction(() -> requests.markConsumed(stateHash, now));

            assertThat(result).isZero();
            assertThat(readConsumedAt(request.getId())).isNull();
        } finally {
            deleteUser(user);
        }
    }

    @Test
    void markConsumedReturnsZeroForUnknownStateHash() {
        Integer result = inNewTransaction(() -> requests.markConsumed(stateHash(), OffsetDateTime.now(ZoneOffset.UTC)));

        assertThat(result).isZero();
    }

    private OAuthAuthorizationRequest saveRequest(
            UUID userId,
            String stateHash,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {
        return inNewTransaction(() -> requests.saveAndFlush(new OAuthAuthorizationRequest(
                userId,
                ConnectedAccountProvider.GOOGLE,
                stateHash,
                "v1:encrypted-code-verifier",
                1,
                List.of("scope-a"),
                createdAt,
                expiresAt)));
    }

    private OffsetDateTime readConsumedAt(UUID requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_authorization_requests WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class),
                requestId);
    }

    private User saveUser() {
        return inNewTransaction(() -> users.saveAndFlush(new User(
                "oauth-consumption-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic OAuth Consumption User",
                new BCryptPasswordEncoder().encode("synthetic-test-password"))));
    }

    private void deleteUser(User user) {
        if (user != null) {
            inNewTransaction(() -> {
                users.deleteById(user.getId());
                return null;
            });
        }
    }

    private <T> T inNewTransaction(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());
    }

    private String stateHash() {
        return UUID.randomUUID().toString().replace("-", "").repeat(2);
    }
}