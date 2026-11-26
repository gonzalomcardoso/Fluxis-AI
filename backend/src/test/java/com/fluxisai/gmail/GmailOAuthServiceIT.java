package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.auth.FluxisUserDetails;
import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(properties = {
        "app.gmail.token-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.gmail.token-encryption-key-version=1",
        "app.gmail.oauth.client-id=test-client-id",
        "app.gmail.oauth.client-secret=test-client-secret",
        "app.gmail.oauth.redirect-uri=http://localhost:8080/api/gmail/oauth/callback",
        "app.gmail.oauth.scopes=openid,email,https://www.googleapis.com/auth/gmail.metadata"
})
@Import(GmailOAuthServiceIT.GoogleOAuthClientTestConfiguration.class)
class GmailOAuthServiceIT extends PostgreSqlIntegrationTestSupport {

    private static final List<String> SCOPES = List.of(
            "openid",
            "email",
            "https://www.googleapis.com/auth/gmail.metadata");
    private static final String ACCESS_TOKEN = "ACCESS-SECRET-123";
    private static final String REFRESH_TOKEN = "REFRESH-SECRET-456";

    @Autowired
    private GmailOAuthService service;

    @Autowired
    private GoogleOAuthClient googleOAuthClient;

    @Autowired
    private OAuthAuthorizationRequestRepository authorizationRequests;

    @Autowired
    private ConnectedAccountRepository connectedAccounts;

    @Autowired
    private UserRepository users;

    @Autowired
    private TokenEncryptionService encryption;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        reset(googleOAuthClient);
        user = saveUser();
        authenticate(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (otherUser != null) {
            users.deleteById(otherUser.getId());
        }
        if (user != null) {
            users.deleteById(user.getId());
        }
    }

    @Test
    void startsOAuthAndPersistsOnlyStateHashAndEncryptedVerifier() {
        when(googleOAuthClient.buildAuthorizationUri(anyString(), anyString(), anyList()))
                .thenAnswer(invocation -> URI.create(
                        "https://accounts.google.com/o/oauth2/v2/auth?state=" + invocation.getArgument(0)));

        URI authorizationUri = service.startAuthorization();

        String state = UriComponentsBuilder.fromUri(authorizationUri)
                .build()
                .getQueryParams()
                .getFirst("state");
        String stateHash = hexSha256(state);
        OAuthAuthorizationRequest request = authorizationRequests.findByStateHash(stateHash).orElseThrow();

        assertThat(state).isNotBlank();
        assertThat(request.getStateHash()).isEqualTo(stateHash);
        assertThat(request.getStateHash()).doesNotContain(state);
        assertThat(request.getCodeVerifierEncrypted()).startsWith("v1:");
        assertThat(encryption.decrypt(
                request.getCodeVerifierEncrypted(),
                request.getEncryptionKeyVersion(),
                "fluxis:gmail-pkce:" + user.getId() + ":" + stateHash)).isNotBlank();
        assertThat(request.getConsumedAt()).isNull();
        assertThat(request.getExpiresAt()).isAfter(request.getCreatedAt());
        assertThat(request.getRequestedScopes()).containsExactlyElementsOf(SCOPES);
        assertThat(authorizationUri.toString()).doesNotContain(request.getCodeVerifierEncrypted());
    }

    @Test
    void callbackConsumesStateAndPersistsEncryptedTokensWithoutReturningTokens() {
        String state = startFlow();
        when(googleOAuthClient.exchangeAuthorizationCode(eq("auth-code"), anyString())).thenReturn(tokens());
        when(googleOAuthClient.fetchUserInfo(ACCESS_TOKEN))
                .thenReturn(new GoogleUserInfo("google-sub-123", "person@example.test", true));

        ConnectedAccountResponse response = service.completeAuthorization("auth-code", state);

        String stateHash = hexSha256(state);
        OffsetDateTime consumedAt = jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_authorization_requests WHERE state_hash = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class),
                stateHash);
        String encryptedAccess = jdbcTemplate.queryForObject(
                "SELECT access_token_encrypted FROM connected_accounts WHERE user_id = ?",
                String.class,
                user.getId());
        String encryptedRefresh = jdbcTemplate.queryForObject(
                "SELECT refresh_token_encrypted FROM connected_accounts WHERE user_id = ?",
                String.class,
                user.getId());
        String aad = "fluxis:gmail-oauth:" + user.getId() + ":google-sub-123";

        assertThat(consumedAt).isNotNull();
        assertThat(response.providerAccountId()).isEqualTo("google-sub-123");
        assertThat(response.accountEmail()).isEqualTo("person@example.test");
        assertThat(response.toString()).doesNotContain(ACCESS_TOKEN, REFRESH_TOKEN);
        assertThat(encryptedAccess).startsWith("v1:").doesNotContain(ACCESS_TOKEN);
        assertThat(encryptedRefresh).startsWith("v1:").doesNotContain(REFRESH_TOKEN);
        assertThat(encryption.decrypt(encryptedAccess, 1, aad)).isEqualTo(ACCESS_TOKEN);
        assertThat(encryption.decrypt(encryptedRefresh, 1, aad)).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    void exchangeFailureLeavesStateConsumedAndCreatesNoConnection() {
        String state = startFlow();
        when(googleOAuthClient.exchangeAuthorizationCode(eq("auth-code"), anyString()))
                .thenThrow(new GoogleOAuthException(GoogleOAuthErrorCode.INVALID_GRANT, "Google token exchange failed"));

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(GoogleOAuthException.class);

        assertThat(consumedAt(state)).isNotNull();
        assertThat(connectionCount()).isZero();
    }

    @Test
    void userInfoFailureLeavesStateConsumedAndCreatesNoConnection() {
        String state = startFlow();
        when(googleOAuthClient.exchangeAuthorizationCode(eq("auth-code"), anyString())).thenReturn(tokens());
        when(googleOAuthClient.fetchUserInfo(ACCESS_TOKEN))
                .thenThrow(new GoogleOAuthException(GoogleOAuthErrorCode.UNAUTHORIZED, "Google UserInfo request failed"));

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(GoogleOAuthException.class);

        assertThat(consumedAt(state)).isNotNull();
        assertThat(connectionCount()).isZero();
    }

    @Test
    void callbackFromAnotherUserIsRejectedWithoutConsumingTheState() {
        String state = startFlow();
        otherUser = saveUser();
        authenticate(otherUser);

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("OAuth authorization request is invalid or expired");

        assertThat(consumedAt(state)).isNull();
        verify(googleOAuthClient, never()).exchangeAuthorizationCode(anyString(), anyString());
    }

    @Test
    void reusedStateIsRejectedAndDoesNotCreateASecondConnection() {
        String state = startFlow();
        when(googleOAuthClient.exchangeAuthorizationCode(eq("auth-code"), anyString())).thenReturn(tokens());
        when(googleOAuthClient.fetchUserInfo(ACCESS_TOKEN))
                .thenReturn(new GoogleUserInfo("google-sub-123", "person@example.test", true));

        service.completeAuthorization("auth-code", state);

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("OAuth authorization request is invalid or expired");
        assertThat(connectionCount()).isEqualTo(1);
    }

    @Test
    void differentGoogleIdentityProducesConflictWithoutReplacingExistingConnection() {
        connectedAccounts.saveAndFlush(new ConnectedAccount(
                user.getId(),
                ConnectedAccountProvider.GOOGLE,
                "existing-google-sub",
                "existing@example.test",
                "v1:existing-access",
                "v1:existing-refresh",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1),
                SCOPES,
                1));
        String state = startFlow();
        when(googleOAuthClient.exchangeAuthorizationCode(eq("auth-code"), anyString())).thenReturn(tokens());
        when(googleOAuthClient.fetchUserInfo(ACCESS_TOKEN))
                .thenReturn(new GoogleUserInfo("different-google-sub", "different@example.test", true));

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(ConflictException.class);

        assertThat(connectionCount()).isEqualTo(1);
        String providerAccountId = jdbcTemplate.queryForObject(
                "SELECT provider_account_id FROM connected_accounts WHERE user_id = ?",
                String.class,
                user.getId());
        assertThat(providerAccountId).isEqualTo("existing-google-sub");
    }

    private String startFlow() {
        when(googleOAuthClient.buildAuthorizationUri(anyString(), anyString(), anyList()))
                .thenAnswer(invocation -> URI.create(
                        "https://accounts.google.com/o/oauth2/v2/auth?state=" + invocation.getArgument(0)));
        URI authorizationUri = service.startAuthorization();
        return UriComponentsBuilder.fromUri(authorizationUri)
                .build()
                .getQueryParams()
                .getFirst("state");
    }

    private GoogleOAuthTokens tokens() {
        return new GoogleOAuthTokens(
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1),
                SCOPES,
                "Bearer");
    }

    private OffsetDateTime consumedAt(String state) {
        return jdbcTemplate.queryForObject(
                "SELECT consumed_at FROM oauth_authorization_requests WHERE state_hash = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class),
                hexSha256(state));
    }

    private int connectionCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM connected_accounts WHERE user_id = ?",
                Integer.class,
                user.getId());
        return count == null ? 0 : count;
    }

    private User saveUser() {
        return users.saveAndFlush(new User(
                "gmail-oauth-it-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic Gmail OAuth User",
                new BCryptPasswordEncoder().encode("synthetic-test-password")));
    }

    private void authenticate(User authenticatedUser) {
        FluxisUserDetails principal = FluxisUserDetails.from(authenticatedUser);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
    }

    private String hexSha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GoogleOAuthClientTestConfiguration {

        @Bean
        @Primary
        GoogleOAuthClient testGoogleOAuthClient() {
            return mock(GoogleOAuthClient.class);
        }
    }
}