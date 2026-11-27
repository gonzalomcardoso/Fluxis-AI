package com.fluxisai.gmail;

import com.fluxisai.audit.AuditEventService;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GmailConnectionServiceTest {

    private ConnectedAccountService connectedAccounts;
    private GoogleOAuthClient googleOAuthClient;
    private AuditEventService audit;
    private GmailConnectionService service;

    @BeforeEach
    void setUp() {
        connectedAccounts = mock(ConnectedAccountService.class);
        googleOAuthClient = mock(GoogleOAuthClient.class);
        audit = mock(AuditEventService.class);
        service = new GmailConnectionService(connectedAccounts, googleOAuthClient, audit);
    }
    @Test
    void revokesRefreshTokenAndAlwaysCleansLocally() {
        when(connectedAccounts.currentUserTokens()).thenReturn(Optional.of(tokens("access", "refresh")));

        service.disconnectCurrentUser();

        verify(googleOAuthClient).revokeToken("refresh");
        verify(connectedAccounts).disconnectCurrentUser();
        verify(audit).record("GMAIL_DISCONNECT_SUCCESS", "GMAIL", null, "DISCONNECT", java.util.Collections.emptyMap());
    }

    @Test
    void fallsBackToAccessTokenWhenRefreshTokenIsMissing() {
        when(connectedAccounts.currentUserTokens()).thenReturn(Optional.of(tokens("access", null)));

        service.disconnectCurrentUser();

        verify(googleOAuthClient).revokeToken("access");
        verify(connectedAccounts).disconnectCurrentUser();
        verify(audit).record("GMAIL_DISCONNECT_SUCCESS", "GMAIL", null, "DISCONNECT", java.util.Collections.emptyMap());
    }

    @Test
    void remoteRevocationFailureDoesNotPreventLocalCleanup() {
        when(connectedAccounts.currentUserTokens()).thenReturn(Optional.of(tokens("access", "refresh")));
        doThrow(new GoogleOAuthException(GoogleOAuthErrorCode.UPSTREAM_ERROR, "revocation failed"))
                .when(googleOAuthClient).revokeToken("refresh");

        assertThatCode(service::disconnectCurrentUser).doesNotThrowAnyException();

        verify(connectedAccounts).disconnectCurrentUser();
        verify(audit).record("GMAIL_DISCONNECT_SUCCESS", "GMAIL", null, "DISCONNECT", java.util.Collections.emptyMap());
    }

    @Test
    void missingConnectionStillRunsLocalCleanup() {
        when(connectedAccounts.currentUserTokens()).thenReturn(Optional.empty());

        service.disconnectCurrentUser();

        verify(googleOAuthClient, never()).revokeToken(org.mockito.ArgumentMatchers.anyString());
        verify(connectedAccounts).disconnectCurrentUser();
        verify(audit).record("GMAIL_DISCONNECT_SUCCESS", "GMAIL", null, "DISCONNECT", java.util.Collections.emptyMap());
    }

    @Test
    void tokenReadFailureStillRunsLocalCleanup() {
        when(connectedAccounts.currentUserTokens()).thenThrow(new IllegalStateException("token read failed"));

        assertThatCode(service::disconnectCurrentUser).doesNotThrowAnyException();

        verify(connectedAccounts).disconnectCurrentUser();
        // Since it's a RuntimeException, it logs the failure
        verify(audit).record("GMAIL_DISCONNECT_FAILED", "GMAIL", null, "DISCONNECT", java.util.Collections.emptyMap());
    }

    private OAuthTokens tokens(String accessToken, String refreshToken) {
        return new OAuthTokens(
                accessToken,
                refreshToken,
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1),
                List.of(GmailOAuthScopes.GMAIL_METADATA));
    }
}