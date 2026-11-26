package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.common.security.CurrentUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class GmailOAuthServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW_INSTANT = Instant.parse("2026-11-25T12:00:00Z");
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);
    private static final List<String> SCOPES = List.of(
            "openid",
            "email",
            "https://www.googleapis.com/auth/gmail.metadata");

    private CurrentUser currentUser;
    private GmailOAuthProperties properties;
    private OAuthAuthorizationRequestStore authorizationRequests;
    private TokenEncryptionService encryption;
    private GoogleOAuthClient googleOAuthClient;
    private ConnectedAccountService connectedAccountService;
    private GmailOAuthService service;

    @BeforeEach
    void setUp() {
        currentUser = mock(CurrentUser.class);
        properties = new GmailOAuthProperties(
                "test-client-id",
                "test-client-secret",
                "http://localhost:8080/api/gmail/oauth/callback",
                SCOPES);
        authorizationRequests = mock(OAuthAuthorizationRequestStore.class);
        encryption = new TokenEncryptionService(KEY, 1);
        googleOAuthClient = mock(GoogleOAuthClient.class);
        connectedAccountService = mock(ConnectedAccountService.class);
        when(currentUser.id()).thenReturn(USER_ID);
        service = new GmailOAuthService(
                currentUser,
                properties,
                authorizationRequests,
                encryption,
                googleOAuthClient,
                connectedAccountService,
                Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));
    }

    @Test
    void startAuthorizationPersistsHashEncryptedVerifierChallengeAndExpiry() {
        URI expectedUri = URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=test");
        when(googleOAuthClient.buildAuthorizationUri(anyString(), anyString(), anyList()))
                .thenReturn(expectedUri);

        URI result = service.startAuthorization();

        assertThat(result).isEqualTo(expectedUri);

        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> challengeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> scopesCaptor = ArgumentCaptor.forClass(List.class);
        verify(googleOAuthClient).buildAuthorizationUri(
                stateCaptor.capture(), challengeCaptor.capture(), scopesCaptor.capture());

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> encryptedVerifierCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> createdAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> expiresAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(authorizationRequests).save(
                eq(USER_ID),
                eq(ConnectedAccountProvider.GOOGLE),
                hashCaptor.capture(),
                encryptedVerifierCaptor.capture(),
                eq(1),
                eq(SCOPES),
                createdAtCaptor.capture(),
                expiresAtCaptor.capture());

        String state = stateCaptor.getValue();
        String stateHash = hashCaptor.getValue();
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        String verifier = encryption.decrypt(encryptedVerifierCaptor.getValue(), 1, aad);

        assertThat(state).matches("[A-Za-z0-9_-]{43}");
        assertThat(hashCaptor.getValue()).isEqualTo(hexSha256(state));
        assertThat(challengeCaptor.getValue()).isEqualTo(base64UrlSha256(verifier));
        assertThat(scopesCaptor.getValue()).isEqualTo(SCOPES);
        assertThat(createdAtCaptor.getValue()).isEqualTo(NOW);
        assertThat(Duration.between(createdAtCaptor.getValue(), expiresAtCaptor.getValue()))
                .isEqualTo(Duration.ofMinutes(10));
        assertThat(encryptedVerifierCaptor.getValue()).doesNotContain(verifier);
    }

    @Test
    void startAuthorizationGeneratesFreshStateAndVerifier() {
        when(googleOAuthClient.buildAuthorizationUri(anyString(), anyString(), anyList()))
                .thenAnswer(invocation -> URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=test"));

        service.startAuthorization();
        service.startAuthorization();

        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
        verify(googleOAuthClient, times(2)).buildAuthorizationUri(
                stateCaptor.capture(), anyString(), anyList());
        assertThat(stateCaptor.getAllValues().get(0)).isNotEqualTo(stateCaptor.getAllValues().get(1));
        assertThat(stateCaptor.getAllValues()).allMatch(value -> value.matches("[A-Za-z0-9_-]{43}"));
    }

    @Test
    void startAuthorizationDoesNotReturnWhenPersistenceFails() {
        when(googleOAuthClient.buildAuthorizationUri(anyString(), anyString(), anyList()))
                .thenReturn(URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=test"));
        doThrow(new RuntimeException("persistence failure")).when(authorizationRequests).save(
                any(), any(), anyString(), anyString(), anyInt(), anyList(), any(), any());

        assertThatThrownBy(service::startAuthorization).isInstanceOf(RuntimeException.class);
    }

    @Test
    void startAuthorizationRejectsIncompleteConfiguration() {
        GmailOAuthProperties incomplete = new GmailOAuthProperties(
                "", "secret", "http://localhost/callback", SCOPES);
        GmailOAuthService incompleteService = new GmailOAuthService(
                currentUser,
                incomplete,
                authorizationRequests,
                encryption,
                googleOAuthClient,
                connectedAccountService,
                Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));

        assertThatThrownBy(incompleteService::startAuthorization)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gmail OAuth is not configured");
        verifyNoInteractions(googleOAuthClient);
    }

    @Test
    void completeAuthorizationConnectsVerifiedAccountAndDoesNotReturnTokens() {
        String state = "test-state";
        String verifier = "test-verifier";
        String stateHash = hexSha256(state);
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID,
                        encryption.encrypt(verifier, aad),
                        1,
                        SCOPES,
                        NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "ACCESS-SECRET-123",
                "REFRESH-SECRET-456",
                NOW.plusHours(1),
                SCOPES,
                "Bearer");
        GoogleUserInfo userInfo = new GoogleUserInfo(
                "google-sub-123", "person@example.test", true);
        when(googleOAuthClient.exchangeAuthorizationCode("auth-code", verifier)).thenReturn(tokens);
        when(googleOAuthClient.fetchUserInfo("ACCESS-SECRET-123")).thenReturn(userInfo);
        ConnectedAccountResponse response = new ConnectedAccountResponse(
                UUID.randomUUID(),
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                SCOPES,
                NOW.plusHours(1),
                ConnectedAccountStatus.CONNECTED,
                NOW);
        when(connectedAccountService.connectCurrentUser(any(GoogleAccountConnection.class)))
                .thenReturn(response);

        ConnectedAccountResponse result = service.completeAuthorization("auth-code", state);

        assertThat(result).isEqualTo(response);
        assertThat(result.toString()).doesNotContain("ACCESS-SECRET-123", "REFRESH-SECRET-456");
        ArgumentCaptor<GoogleAccountConnection> connectionCaptor =
                ArgumentCaptor.forClass(GoogleAccountConnection.class);
        verify(connectedAccountService).connectCurrentUser(connectionCaptor.capture());
        assertThat(connectionCaptor.getValue().providerAccountId()).isEqualTo("google-sub-123");
        assertThat(connectionCaptor.getValue().accountEmail()).isEqualTo("person@example.test");
        assertThat(connectionCaptor.getValue().accessToken()).isEqualTo("ACCESS-SECRET-123");
        assertThat(connectionCaptor.getValue().refreshToken()).isEqualTo("REFRESH-SECRET-456");
    }

    @Test
    void callbackRejectsMissingExpiredReusedOrUnknownStateWithoutCallingGoogle() {
        when(authorizationRequests.consume(anyString(), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", "unknown-state"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("OAuth authorization request is invalid or expired");
        verifyNoInteractions(googleOAuthClient);
        verifyNoInteractions(connectedAccountService);
    }

    @Test
    void callbackRejectsDecryptFailureWithoutCallingGoogle() {
        String state = "test-state";
        String stateHash = hexSha256(state);
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID, "not-a-ciphertext", 1, SCOPES, NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("OAuth authorization request is invalid or expired");
        verifyNoInteractions(googleOAuthClient);
        verifyNoInteractions(connectedAccountService);
    }

    @Test
    void callbackPropagatesExchangeFailureWithoutCallingUserInfoOrConnecting() {
        String state = "test-state";
        String stateHash = hexSha256(state);
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID, encryption.encrypt("verifier", aad), 1, SCOPES, NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));
        GoogleOAuthException failure = new GoogleOAuthException(
                GoogleOAuthErrorCode.INVALID_GRANT, "Google token exchange failed");
        when(googleOAuthClient.exchangeAuthorizationCode("auth-code", "verifier")).thenThrow(failure);

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isSameAs(failure);
        verify(googleOAuthClient, never()).fetchUserInfo(anyString());
        verifyNoInteractions(connectedAccountService);
    }

    @Test
    void callbackRejectsUnverifiedEmailWithoutConnecting() {
        String state = "test-state";
        String stateHash = hexSha256(state);
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID, encryption.encrypt("verifier", aad), 1, SCOPES, NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));
        when(googleOAuthClient.exchangeAuthorizationCode("auth-code", "verifier"))
                .thenReturn(tokens("access"));
        when(googleOAuthClient.fetchUserInfo("access"))
                .thenReturn(new GoogleUserInfo("sub", "person@example.test", false));

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Google account email is not verified");
        verifyNoInteractions(connectedAccountService);
    }

    @Test
    void callbackPropagatesIdentityConflict() {
        String state = "test-state";
        String stateHash = hexSha256(state);
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID, encryption.encrypt("verifier", aad), 1, SCOPES, NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));
        when(googleOAuthClient.exchangeAuthorizationCode("auth-code", "verifier"))
                .thenReturn(tokens("access"));
        when(googleOAuthClient.fetchUserInfo("access"))
                .thenReturn(new GoogleUserInfo("sub", "person@example.test", true));
        when(connectedAccountService.connectCurrentUser(any()))
                .thenThrow(new ConflictException("different identity"));

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isInstanceOf(ConflictException.class)
                .hasMessage("different identity");
    }

    @Test
    void callbackPropagatesUserInfoFailureWithoutConnecting() {
        String state = "test-state";
        String stateHash = hexSha256(state);
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID, encryption.encrypt("verifier", aad), 1, SCOPES, NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));
        when(googleOAuthClient.exchangeAuthorizationCode("auth-code", "verifier"))
                .thenReturn(tokens("access"));
        GoogleOAuthException failure = new GoogleOAuthException(
                GoogleOAuthErrorCode.UNAUTHORIZED, "Google UserInfo request failed");
        when(googleOAuthClient.fetchUserInfo("access")).thenThrow(failure);

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isSameAs(failure);
        verifyNoInteractions(connectedAccountService);
    }

    @Test
    void callbackPropagatesConnectionPersistenceFailure() {
        String state = "test-state";
        String stateHash = hexSha256(state);
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID, encryption.encrypt("verifier", aad), 1, SCOPES, NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));
        when(googleOAuthClient.exchangeAuthorizationCode("auth-code", "verifier"))
                .thenReturn(tokens("access"));
        when(googleOAuthClient.fetchUserInfo("access"))
                .thenReturn(new GoogleUserInfo("google-sub-123", "person@example.test", true));
        RuntimeException failure = new RuntimeException("connection persistence failed");
        when(connectedAccountService.connectCurrentUser(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.completeAuthorization("auth-code", state))
                .isSameAs(failure);
    }

    @Test
    void successfulCallbackDoesNotLogTokensOrSecrets(CapturedOutput output) {
        String state = "test-state";
        String stateHash = hexSha256(state);
        String aad = "fluxis:gmail-pkce:" + USER_ID + ":" + stateHash;
        OAuthAuthorizationRequestStore.ConsumedAuthorization request =
                new OAuthAuthorizationRequestStore.ConsumedAuthorization(
                        USER_ID, encryption.encrypt("verifier", aad), 1, SCOPES, NOW.plusMinutes(10));
        when(authorizationRequests.consume(eq(stateHash), eq(USER_ID), eq(NOW)))
                .thenReturn(Optional.of(request));
        when(googleOAuthClient.exchangeAuthorizationCode("auth-code", "verifier"))
                .thenReturn(tokens("ACCESS-SECRET-123"));
        when(googleOAuthClient.fetchUserInfo("ACCESS-SECRET-123"))
                .thenReturn(new GoogleUserInfo("google-sub-123", "person@example.test", true));
        when(connectedAccountService.connectCurrentUser(any()))
                .thenReturn(new ConnectedAccountResponse(
                        UUID.randomUUID(),
                        ConnectedAccountProvider.GOOGLE,
                        "google-sub-123",
                        "person@example.test",
                        SCOPES,
                        NOW.plusHours(1),
                        ConnectedAccountStatus.CONNECTED,
                        NOW));

        service.completeAuthorization("auth-code", state);

        assertThat(output.getOut()).doesNotContain("ACCESS-SECRET-123", "REFRESH-SECRET-456");
        assertThat(output.getErr()).doesNotContain("ACCESS-SECRET-123", "REFRESH-SECRET-456");
    }

    private GoogleOAuthTokens tokens(String accessToken) {
        return new GoogleOAuthTokens(accessToken, "REFRESH-SECRET-456", NOW.plusHours(1), SCOPES, "Bearer");
    }

    private String hexSha256(String value) {
        return HexFormat.of().formatHex(sha256(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64UrlSha256(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sha256(value.getBytes(StandardCharsets.US_ASCII)));
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}