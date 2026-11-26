package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GoogleOAuthTokensTest {

    private static final OffsetDateTime EXPIRES_AT =
            OffsetDateTime.of(2026, 11, 25, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void toStringDoesNotExposeTokensWhenBothArePresent() {
        List<String> scopes = List.of("scope-a", "scope-b");
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "ACCESS-SECRET-123",
                "REFRESH-SECRET-456",
                EXPIRES_AT,
                scopes,
                "Bearer");

        String result = tokens.toString();

        assertThat(result)
                .doesNotContain("ACCESS-SECRET-123", "REFRESH-SECRET-456")
                .contains(
                        "hasRefreshToken=true",
                        "expiresAt=" + EXPIRES_AT,
                        "scopes=" + scopes,
                        "tokenType=Bearer");
    }

    @Test
    void toStringDoesNotExposeAccessTokenWhenRefreshTokenIsNull() {
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "ACCESS-SECRET-123",
                null,
                EXPIRES_AT,
                List.of("scope-a"),
                "Bearer");

        String result = tokens.toString();

        assertThat(result)
                .doesNotContain("ACCESS-SECRET-123")
                .contains("hasRefreshToken=false");
    }

    @Test
    void scopesAreImmutable() {
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "ACCESS-SECRET-123",
                null,
                EXPIRES_AT,
                new ArrayList<>(List.of("scope-a")),
                "Bearer");

        assertThatThrownBy(() -> tokens.scopes().add("scope-b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void scopesAreCopiedDefensively() {
        List<String> originalScopes = new ArrayList<>(List.of("scope-a"));
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "ACCESS-SECRET-123",
                null,
                EXPIRES_AT,
                originalScopes,
                "Bearer");

        originalScopes.add("scope-b");

        assertThat(tokens.scopes()).containsExactly("scope-a");
    }

    @Test
    void nullScopesBecomeAnEmptyList() {
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "ACCESS-SECRET-123",
                null,
                EXPIRES_AT,
                null,
                "Bearer");

        assertThat(tokens.scopes()).isEmpty();
    }

    @Test
    void rejectsNullOrBlankAccessTokenWithLiteralSafeMessage() {
        assertThatThrownBy(() -> new GoogleOAuthTokens(null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access token is required");

        assertThatThrownBy(() -> new GoogleOAuthTokens("", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access token is required");

        assertThatThrownBy(() -> new GoogleOAuthTokens("   ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access token is required");
    }

    @Test
    void acceptsNullRefreshTokenExpiresAtAndTokenType() {
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "ACCESS-SECRET-123",
                null,
                null,
                List.of("scope-a"),
                null);

        assertThat(tokens.refreshToken()).isNull();
        assertThat(tokens.expiresAt()).isNull();
        assertThat(tokens.tokenType()).isNull();
    }
}