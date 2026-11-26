package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAuthTokensTest {

    @Test
    void toStringDoesNotExposeTokensWhenBothArePresent() {
        OffsetDateTime expiresAt = OffsetDateTime.of(2026, 11, 25, 12, 0, 0, 0, ZoneOffset.UTC);
        List<String> scopes = List.of("scope-a", "scope-b");
        OAuthTokens tokens = new OAuthTokens(
                "ACCESS-SECRET-123",
                "REFRESH-SECRET-456",
                expiresAt,
                scopes);

        String result = tokens.toString();

        assertThat(result)
                .doesNotContain("ACCESS-SECRET-123", "REFRESH-SECRET-456")
                .contains("hasRefreshToken=true", "expiresAt=" + expiresAt, "scopes=" + scopes);
    }

    @Test
    void toStringDoesNotExposeAccessTokenWhenRefreshTokenIsNull() {
        OffsetDateTime expiresAt = OffsetDateTime.of(2026, 11, 25, 13, 0, 0, 0, ZoneOffset.UTC);
        OAuthTokens tokens = new OAuthTokens(
                "ACCESS-SECRET-123",
                null,
                expiresAt,
                List.of("scope-a"));

        String result = tokens.toString();

        assertThat(result)
                .doesNotContain("ACCESS-SECRET-123")
                .contains("hasRefreshToken=false", "expiresAt=" + expiresAt, "scopes=[scope-a]");
    }
}