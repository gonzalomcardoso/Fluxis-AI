package com.fluxisai.gmail;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Tokens decrypted in backend memory only. Never serialize this record from a
 * controller or expose it to the frontend.
 */
public record OAuthTokens(
        String accessToken,
        String refreshToken,
        OffsetDateTime expiresAt,
        List<String> scopes) {

    public OAuthTokens {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
}
