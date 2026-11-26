package com.fluxisai.gmail;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Represents the token response returned by Google's OAuth token endpoint.
 *
 * <p>This type is distinct from {@link OAuthTokens}, which represents decrypted
 * tokens for internal use. Conversion between both types is intentionally not
 * performed in this task.</p>
 */
public record GoogleOAuthTokens(
        String accessToken,
        String refreshToken,
        OffsetDateTime expiresAt,
        List<String> scopes,
        String tokenType) {

    public GoogleOAuthTokens {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    /**
     * Returns a safe representation that never includes token values.
     */
    @Override
    public String toString() {
        return "GoogleOAuthTokens[hasRefreshToken=%s, expiresAt=%s, scopes=%s, tokenType=%s]"
                .formatted(refreshToken != null, expiresAt, scopes, tokenType);
    }
}