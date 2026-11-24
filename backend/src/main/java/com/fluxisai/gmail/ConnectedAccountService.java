package com.fluxisai.gmail;

import java.util.Optional;
import java.util.UUID;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns Gmail connection metadata and keeps OAuth credentials encrypted.
 * HTTP OAuth endpoints are intentionally added in the next integration step.
 */
@Service
public class ConnectedAccountService {

    private final ConnectedAccountRepository accounts;
    private final TokenEncryptionService encryption;
    private final CurrentUser currentUser;

    public ConnectedAccountService(
            ConnectedAccountRepository accounts,
            TokenEncryptionService encryption,
            CurrentUser currentUser) {
        this.accounts = accounts;
        this.encryption = encryption;
        this.currentUser = currentUser;
    }

    @Transactional
    public ConnectedAccountResponse connectCurrentUser(GoogleAccountConnection connection) {
        UUID userId = currentUser.id();
        ConnectedAccount account = accounts
                .findByUserIdAndProvider(userId, ConnectedAccountProvider.GOOGLE)
                .orElse(null);

        if (account != null && !account.getProviderAccountId().equals(connection.providerAccountId())) {
            throw new ConflictException("The connected Google account cannot be replaced by a different identity");
        }

        String associatedData = associatedData(userId, connection.providerAccountId());
        String encryptedAccessToken = encryption.encrypt(connection.accessToken(), associatedData);
        String encryptedRefreshToken;
        if (connection.refreshToken() != null && !connection.refreshToken().isBlank()) {
            encryptedRefreshToken = encryption.encrypt(connection.refreshToken(), associatedData);
        } else if (account != null && account.getRefreshTokenEncrypted() != null) {
            String previousRefreshToken = encryption.decrypt(
                    account.getRefreshTokenEncrypted(),
                    account.getEncryptionKeyVersion(),
                    associatedData);
            encryptedRefreshToken = encryption.encrypt(previousRefreshToken, associatedData);
        } else {
            encryptedRefreshToken = null;
        }

        if (account == null) {
            account = new ConnectedAccount(
                    userId,
                    ConnectedAccountProvider.GOOGLE,
                    connection.providerAccountId(),
                    connection.accountEmail(),
                    encryptedAccessToken,
                    encryptedRefreshToken,
                    connection.tokenExpiresAt(),
                    connection.grantedScopes(),
                    encryption.keyVersion());
        } else {
            account.updateConnection(
                    connection.accountEmail(),
                    encryptedAccessToken,
                    encryptedRefreshToken,
                    connection.tokenExpiresAt(),
                    connection.grantedScopes(),
                    encryption.keyVersion());
        }

        return ConnectedAccountResponse.from(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public Optional<ConnectedAccountResponse> currentUserConnection() {
        return accounts
                .findByUserIdAndProvider(currentUser.id(), ConnectedAccountProvider.GOOGLE)
                .map(ConnectedAccountResponse::from);
    }

    /**
     * Decrypts credentials for a backend Gmail operation. Never return this
     * value from a controller or write it to logs.
     */
    @Transactional(readOnly = true)
    public Optional<OAuthTokens> currentUserTokens() {
        return accounts
                .findByUserIdAndProvider(currentUser.id(), ConnectedAccountProvider.GOOGLE)
                .filter(account -> account.getStatus() == ConnectedAccountStatus.CONNECTED)
                .map(this::decrypt);
    }

    @Transactional
    public void disconnectCurrentUser() {
        accounts
                .findByUserIdAndProvider(currentUser.id(), ConnectedAccountProvider.GOOGLE)
                .ifPresent(account -> {
                    account.revoke();
                    accounts.save(account);
                });
    }

    private OAuthTokens decrypt(ConnectedAccount account) {
        String accessTokenEncrypted = account.getAccessTokenEncrypted();
        if (accessTokenEncrypted == null) {
            throw new IllegalStateException("Connected Google account has no access token");
        }
        String associatedData = associatedData(account.getUserId(), account.getProviderAccountId());
        String refreshToken = account.getRefreshTokenEncrypted() == null
                ? null
                : encryption.decrypt(
                        account.getRefreshTokenEncrypted(),
                        account.getEncryptionKeyVersion(),
                        associatedData);
        return new OAuthTokens(
                encryption.decrypt(
                        accessTokenEncrypted,
                        account.getEncryptionKeyVersion(),
                        associatedData),
                refreshToken,
                account.getTokenExpiresAt(),
                account.getGrantedScopes());
    }

    private static String associatedData(UUID userId, String providerAccountId) {
        return "fluxis:gmail-oauth:" + userId + ":" + providerAccountId;
    }
}
