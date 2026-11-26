package com.fluxisai.gmail;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.common.security.CurrentUser;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Coordinates the Gmail OAuth authorization flow.
 */
@Service
public class GmailOAuthService {

    private static final int RANDOM_BYTES = 32;
    private static final Duration AUTHORIZATION_TTL = Duration.ofMinutes(10);

    private final CurrentUser currentUser;
    private final GmailOAuthProperties properties;
    private final OAuthAuthorizationRequestStore authorizationRequests;
    private final TokenEncryptionService encryption;
    private final GoogleOAuthClient googleOAuthClient;
    private final ConnectedAccountService connectedAccountService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public GmailOAuthService(
            CurrentUser currentUser,
            GmailOAuthProperties properties,
            OAuthAuthorizationRequestStore authorizationRequests,
            TokenEncryptionService encryption,
            GoogleOAuthClient googleOAuthClient,
            ConnectedAccountService connectedAccountService,
            @Qualifier("gmailOAuthClock") Clock clock) {
        this.currentUser = currentUser;
        this.properties = properties;
        this.authorizationRequests = authorizationRequests;
        this.encryption = encryption;
        this.googleOAuthClient = googleOAuthClient;
        this.connectedAccountService = connectedAccountService;
        this.clock = clock;
    }

    public URI startAuthorization() {
        requireConfigured();
        List<String> scopes = properties.scopes();

        UUID userId = currentUser.id();
        String state = randomToken();
        String codeVerifier = randomToken();
        String codeChallenge = base64UrlSha256(codeVerifier);
        String stateHash = hexSha256(state);
        String associatedData = associatedData(userId, stateHash);
        String encryptedVerifier = encryption.encrypt(codeVerifier, associatedData);

        OffsetDateTime createdAt = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = createdAt.plus(AUTHORIZATION_TTL);
        URI authorizationUri = googleOAuthClient.buildAuthorizationUri(state, codeChallenge, scopes);

        authorizationRequests.save(
                userId,
                ConnectedAccountProvider.GOOGLE,
                stateHash,
                encryptedVerifier,
                encryption.keyVersion(),
                scopes,
                createdAt,
                expiresAt);

        return authorizationUri;
    }

    public ConnectedAccountResponse completeAuthorization(String code, String state) {
        requireCallbackText(code, "Authorization code is required");
        requireCallbackText(state, "OAuth state is required");

        UUID userId = currentUser.id();
        String stateHash = hexSha256(state);
        OffsetDateTime now = OffsetDateTime.now(clock);

        OAuthAuthorizationRequestStore.ConsumedAuthorization request = authorizationRequests
                .consume(stateHash, userId, now)
                .orElseThrow(GmailOAuthService::invalidAuthorizationRequest);

        String codeVerifier;
        try {
            codeVerifier = encryption.decrypt(
                    request.codeVerifierEncrypted(),
                    request.encryptionKeyVersion(),
                    associatedData(userId, stateHash));
        } catch (RuntimeException exception) {
            throw invalidAuthorizationRequest();
        }

        GoogleOAuthTokens tokens = googleOAuthClient.exchangeAuthorizationCode(code, codeVerifier);
        GoogleUserInfo userInfo = googleOAuthClient.fetchUserInfo(tokens.accessToken());

        if (!userInfo.emailVerified()) {
            throw new InvalidRequestException("Google account email is not verified");
        }

        GoogleAccountConnection connection;
        try {
            connection = new GoogleAccountConnection(
                    userInfo.subject(),
                    userInfo.email(),
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.expiresAt(),
                    tokens.scopes());
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Google OAuth response is invalid");
        }

        return connectedAccountService.connectCurrentUser(connection);
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Gmail OAuth is not configured");
        }
        List<String> scopes = properties.scopes();
        if (scopes == null || scopes.isEmpty()
                || scopes.stream().anyMatch(scope -> scope == null || scope.isBlank())) {
            throw new IllegalStateException("Gmail OAuth scopes are not configured");
        }
    }

    private void requireCallbackText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(message);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String base64UrlSha256(String value) {
        byte[] digest = sha256(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String hexSha256(String value) {
        return HexFormat.of().formatHex(sha256(value.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available");
        }
    }

    private String associatedData(UUID userId, String stateHash) {
        return "fluxis:gmail-pkce:" + userId + ":" + stateHash;
    }

    private static InvalidRequestException invalidAuthorizationRequest() {
        return new InvalidRequestException("OAuth authorization request is invalid or expired");
    }
}