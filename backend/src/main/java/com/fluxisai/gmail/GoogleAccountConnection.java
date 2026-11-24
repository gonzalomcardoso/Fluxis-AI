package com.fluxisai.gmail;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Input produced by the OAuth callback after validating Google's response.
 */
public record GoogleAccountConnection(
        String providerAccountId,
        String accountEmail,
        String accessToken,
        String refreshToken,
        OffsetDateTime tokenExpiresAt,
        List<String> grantedScopes) {

    public GoogleAccountConnection {
        if (providerAccountId == null || providerAccountId.isBlank()) {
            throw new IllegalArgumentException("Google account identifier is required");
        }
        if (accountEmail == null || accountEmail.isBlank()) {
            throw new IllegalArgumentException("Google account email is required");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
        grantedScopes = grantedScopes == null
                ? List.of()
                : grantedScopes.stream().filter(scope -> scope != null && !scope.isBlank()).distinct().toList();
    }
}
