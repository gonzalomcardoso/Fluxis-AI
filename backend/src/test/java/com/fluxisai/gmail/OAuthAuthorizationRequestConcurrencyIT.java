package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
class OAuthAuthorizationRequestConcurrencyIT extends PostgreSqlIntegrationTestSupport {

    private static final int THREAD_COUNT = 8;
    private static final long START_TIMEOUT_SECONDS = 10;
    private static final long FUTURE_TIMEOUT_SECONDS = 15;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    @Autowired
    private OAuthAuthorizationRequestRepository requests;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void onlyOneConcurrentCallConsumesTheAuthorizationRequest() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        User user = null;

        try {
            user = saveUser();
            String stateHash = stateHash();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OAuthAuthorizationRequest request = saveRequest(
                    user.getId(), stateHash, now.minusMinutes(5), now.plusHours(1));

            CountDownLatch readyGate = new CountDownLatch(THREAD_COUNT);
            for (int index = 0; index < THREAD_COUNT; index++) {
                futures.add(executor.submit(() -> {
                    readyGate.countDown();
                    if (!startGate.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent start");
                    }
                    return inNewTransaction(() -> requests.markConsumed(stateHash, now));
                }));
            }

            assertThat(readyGate.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("All concurrent workers must reach the start barrier")
                    .isTrue();
            startGate.countDown();

            List<Integer> results = new ArrayList<>();
            List<Throwable> failures = new ArrayList<>();
            for (Future<Integer> future : futures) {
                try {
                    results.add(future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failures.add(exception);
                } catch (ExecutionException | TimeoutException exception) {
                    failures.add(exception);
                }
            }

            assertThat(failures)
                    .withFailMessage(() -> "Concurrent consumption failed: "
                            + failures.stream().map(Throwable::toString).collect(Collectors.joining("; ")))
                    .isEmpty();
            assertThat(results).hasSize(THREAD_COUNT);
            assertThat(Collections.frequency(results, 1)).isEqualTo(1);
            assertThat(Collections.frequency(results, 0)).isEqualTo(THREAD_COUNT - 1);
            assertThat(readConsumedAt(request.getId())).isNotNull();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while starting concurrent workers", exception);
        } finally {
            startGate.countDown();
            for (Future<Integer> future : futures) {
                future.cancel(true);
            }
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
            deleteUser(user);
        }
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
                "oauth-concurrency-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic OAuth Concurrency User",
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