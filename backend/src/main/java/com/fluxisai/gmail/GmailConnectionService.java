package com.fluxisai.gmail;

import com.fluxisai.audit.AuditEventService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * Coordinates remote Google revocation and local credential cleanup.
 */
@Service
public class GmailConnectionService {

    private final ConnectedAccountService connectedAccounts;
    private final GoogleOAuthClient googleOAuthClient;
    private final AuditEventService audit;

    public GmailConnectionService(
            ConnectedAccountService connectedAccounts,
            GoogleOAuthClient googleOAuthClient,
            AuditEventService audit) {
        this.connectedAccounts = connectedAccounts;
        this.googleOAuthClient = googleOAuthClient;
        this.audit = audit;
    }

    public void disconnectCurrentUser() {
        try {
            Optional<OAuthTokens> tokens = connectedAccounts.currentUserTokens();
            tokens.ifPresent(this::revokeQuietly);
            audit.record("GMAIL_DISCONNECT_SUCCESS", "GMAIL", null, "DISCONNECT", Collections.emptyMap());
        } catch (RuntimeException e) {
            audit.record("GMAIL_DISCONNECT_FAILED", "GMAIL", null, "DISCONNECT", Collections.emptyMap());
            // No hacemos throw e;
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