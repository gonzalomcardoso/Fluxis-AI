package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.security.CurrentUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConnectedAccountServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.of(
            2026, 11, 22, 12, 0, 0, 0, ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();
    private ConnectedAccountRepository accounts;
    private TokenEncryptionService encryption;
    private CurrentUser currentUser;
    private ConnectedAccountService service;

    @BeforeEach
    void setUp() {
        accounts = mock(ConnectedAccountRepository.class);
        encryption = new TokenEncryptionService(KEY, 1);
        currentUser = mock(CurrentUser.class);
        service = new ConnectedAccountService(accounts, encryption, currentUser);
        when(currentUser.id()).thenReturn(userId);
    }

    @Test
    void storesOnlyEncryptedOAuthTokens() {
        when(accounts.findByUserIdAndProvider(userId, ConnectedAccountProvider.GOOGLE))
                .thenReturn(Optional.empty());
        when(accounts.save(any(ConnectedAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        GoogleAccountConnection connection = new GoogleAccountConnection(
                "google-sub-123",
                "person@example.test",
                "plain-access-token",
                "plain-refresh-token",
                EXPIRES_AT,
                GmailOAuthScopes.BASELINE);

        ConnectedAccountResponse response = service.connectCurrentUser(connection);

        ArgumentCaptor<ConnectedAccount> accountCaptor = ArgumentCaptor.forClass(ConnectedAccount.class);
        verify(accounts).save(accountCaptor.capture());
        ConnectedAccount saved = accountCaptor.getValue();
        assertThat(saved.getAccessTokenEncrypted())
                .startsWith("v1:")
                .doesNotContain("plain-access-token");
        assertThat(saved.getRefreshTokenEncrypted())
                .startsWith("v1:")
                .doesNotContain("plain-refresh-token");
        assertThat(saved.getEncryptionKeyVersion()).isEqualTo(1);
        assertThat(response).extracting(ConnectedAccountResponse::accountEmail)
                .isEqualTo("person@example.test");
        assertThat(response.toString()).doesNotContain("plain-access-token").doesNotContain("plain-refresh-token");
    }

    @Test
    void decryptsTokensOnlyForBackendOperations() {
        String associatedData = "fluxis:gmail-oauth:" + userId + ":google-sub-123";
        ConnectedAccount account = new ConnectedAccount(
                userId,
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                encryption.encrypt("plain-access-token", associatedData),
                encryption.encrypt("plain-refresh-token", associatedData),
                EXPIRES_AT,
                GmailOAuthScopes.BASELINE,
                1);
        when(accounts.findByUserIdAndProvider(userId, ConnectedAccountProvider.GOOGLE))
                .thenReturn(Optional.of(account));

        OAuthTokens tokens = service.currentUserTokens().orElseThrow();

        assertThat(tokens.accessToken()).isEqualTo("plain-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("plain-refresh-token");
        assertThat(tokens.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(tokens.scopes()).containsExactlyElementsOf(GmailOAuthScopes.BASELINE);
    }

    @Test
    void preservesExistingRefreshTokenWhenGoogleOmitsItOnReconnect() {
        String associatedData = "fluxis:gmail-oauth:" + userId + ":google-sub-123";
        ConnectedAccount account = new ConnectedAccount(
                userId,
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                encryption.encrypt("old-access-token", associatedData),
                encryption.encrypt("old-refresh-token", associatedData),
                EXPIRES_AT,
                GmailOAuthScopes.BASELINE,
                1);
        when(accounts.findByUserIdAndProvider(userId, ConnectedAccountProvider.GOOGLE))
                .thenReturn(Optional.of(account));
        when(accounts.save(any(ConnectedAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.connectCurrentUser(new GoogleAccountConnection(
                "google-sub-123",
                "person@example.test",
                "new-access-token",
                null,
                EXPIRES_AT.plusHours(1),
                GmailOAuthScopes.BASELINE));

        OAuthTokens tokens = service.currentUserTokens().orElseThrow();
        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("old-refresh-token");
    }

    @Test
    void disconnectWipesTokensAndMarksAccountRevoked() {
        ConnectedAccount account = new ConnectedAccount(
                userId,
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                "encrypted-access",
                "encrypted-refresh",
                EXPIRES_AT,
                GmailOAuthScopes.BASELINE,
                1);
        when(accounts.findByUserIdAndProvider(userId, ConnectedAccountProvider.GOOGLE))
                .thenReturn(Optional.of(account));
        when(accounts.save(any(ConnectedAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.disconnectCurrentUser();

        assertThat(account.getStatus()).isEqualTo(ConnectedAccountStatus.REVOKED);
        assertThat(account.getAccessTokenEncrypted()).isNull();
        assertThat(account.getRefreshTokenEncrypted()).isNull();
        assertThat(account.getRevokedAt()).isNotNull();
    }

    @Test
    void refusesToReplaceAConnectedGoogleIdentity() {
        ConnectedAccount account = new ConnectedAccount(
                userId,
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                "encrypted-access",
                null,
                EXPIRES_AT,
                GmailOAuthScopes.BASELINE,
                1);
        when(accounts.findByUserIdAndProvider(userId, ConnectedAccountProvider.GOOGLE))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.connectCurrentUser(new GoogleAccountConnection(
                "different-google-sub",
                "other@example.test",
                "token",
                null,
                EXPIRES_AT,
                GmailOAuthScopes.BASELINE)))
                .isInstanceOf(ConflictException.class);
    }
}

