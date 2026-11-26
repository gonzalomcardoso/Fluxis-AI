package com.fluxisai.gmail;

import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Coordinates remote Google revocation and local credential cleanup.
 */
@Service
public class GmailConnectionService {

    private final ConnectedAccountService connectedAccounts;
    private final GoogleOAuthClient googleOAuthClient;

    public GmailConnectionService(
            ConnectedAccountService connectedAccounts,
            GoogleOAuthClient googleOAuthClient) {
        this.connectedAccounts = connectedAccounts;
        this.googleOAuthClient = googleOAuthClient;
    }

    public void disconnectCurrentUser() {
        try {
            Optional<OAuthTokens> tokens = connectedAccounts.currentUserTokens();
            tokens.ifPresent(this::revokeQuietly);
        } catch (RuntimeException ignored) {
            // Remote failures must never prevent local credential cleanup.
        } finally {
            connectedAccounts.disconnectCurrentUser();
        }
    }

    private void revokeQuietly(OAuthTokens tokens) {
        String token = tokens.refreshToken() != null && !tokens.refreshToken().isBlank()
                ? tokens.refreshToken()
                : tokens.accessToken();
        try {
            googleOAuthClient.revokeToken(token);
        } catch (RuntimeException ignored) {
            // Local cleanup still proceeds when Google cannot revoke the token.
        }
    }
}