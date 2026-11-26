package com.fluxisai.gmail;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OAuth client backed by Spring's {@link RestClient}.
 */
public class RestClientGoogleOAuthClient implements GoogleOAuthClient {

    private static final String AUTHORIZATION_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String REVOCATION_ENDPOINT = "https://oauth2.googleapis.com/revoke";

    private static final String TOKEN_EXCHANGE_OPERATION = "Google token exchange";
    private static final String TOKEN_REFRESH_OPERATION = "Google token refresh";
    private static final String USERINFO_OPERATION = "Google UserInfo request";
    private static final String REVOCATION_OPERATION = "Google token revocation";

    private final RestClient restClient;
    private final GmailOAuthProperties properties;
    private final Clock clock;

    public RestClientGoogleOAuthClient(
            RestClient restClient,
            GmailOAuthProperties properties,
            Clock clock) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public URI buildAuthorizationUri(String state, String codeChallenge, List<String> scopes) {
        requireText(state, "OAuth state is required");
        requireText(codeChallenge, "PKCE code challenge is required");
        requireScopes(scopes);
        requireAuthorizationConfiguration();

        return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", scopes))
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    @Override
    public GoogleOAuthTokens exchangeAuthorizationCode(String code, String codeVerifier) {
        requireText(code, "Authorization code is required");
        requireText(codeVerifier, "PKCE code verifier is required");
        requireTokenConfiguration();

        MultiValueMap<String, String> form = formData(
                "code", code,
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret(),
                "redirect_uri", properties.redirectUri(),
                "grant_type", "authorization_code",
                "code_verifier", codeVerifier);

        return requestTokens(form, TOKEN_EXCHANGE_OPERATION);
    }

    @Override
    public GoogleOAuthTokens refreshAccessToken(String refreshToken) {
        requireText(refreshToken, "Refresh token is required");
        requireTokenConfiguration();

        MultiValueMap<String, String> form = formData(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret());

        return requestTokens(form, TOKEN_REFRESH_OPERATION);
    }

    @Override
    public GoogleUserInfo fetchUserInfo(String accessToken) {
        requireText(accessToken, "Access token is required");

        try {
            return restClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return toUserInfo(readUserInfoPayload(response), USERINFO_OPERATION);
                        }
                        throw httpFailure(USERINFO_OPERATION, response);
                    });
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw httpFailure(USERINFO_OPERATION, exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw networkFailure(USERINFO_OPERATION);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(USERINFO_OPERATION);
        }
    }

    @Override
    public void revokeToken(String token) {
        requireText(token, "Token to revoke is required");

        MultiValueMap<String, String> form = formData("token", token);

        try {
            restClient.post()
                    .uri(REVOCATION_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return null;
                        }
                        throw httpFailure(REVOCATION_OPERATION, response);
                    });
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw httpFailure(REVOCATION_OPERATION, exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw networkFailure(REVOCATION_OPERATION);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(REVOCATION_OPERATION);
        }
    }

    private GoogleOAuthTokens requestTokens(MultiValueMap<String, String> form, String operation) {
        try {
            return restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return toTokens(readTokenPayload(response, operation), operation);
                        }
                        throw httpFailure(operation, response);
                    });
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw httpFailure(operation, exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw networkFailure(operation);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(operation);
        }
    }

    private GoogleTokenPayload readTokenPayload(
            ConvertibleClientHttpResponse response,
            String operation) {
        try {
            GoogleTokenPayload payload = response.bodyTo(GoogleTokenPayload.class);
            if (payload == null) {
                throw invalidResponse(operation);
            }
            return payload;
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidResponse(operation);
        }
    }

    private GoogleUserInfoPayload readUserInfoPayload(ConvertibleClientHttpResponse response) {
        try {
            GoogleUserInfoPayload payload = response.bodyTo(GoogleUserInfoPayload.class);
            if (payload == null) {
                throw invalidResponse(USERINFO_OPERATION);
            }
            return payload;
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidResponse(USERINFO_OPERATION);
        }
    }

    private GoogleOAuthTokens toTokens(GoogleTokenPayload payload, String operation) {
        if (payload.accessToken() == null || payload.accessToken().isBlank()) {
            throw invalidResponse(operation);
        }

        try {
            OffsetDateTime expiresAt = null;
            if (payload.expiresIn() != null) {
                if (payload.expiresIn() < 0) {
                    throw invalidResponse(operation);
                }
                expiresAt = OffsetDateTime.ofInstant(
                        clock.instant().plusSeconds(payload.expiresIn()),
                        ZoneOffset.UTC);
            }
            return new GoogleOAuthTokens(
                    payload.accessToken(),
                    payload.refreshToken(),
                    expiresAt,
                    parseScopes(payload.scope()),
                    payload.tokenType());
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (DateTimeException | ArithmeticException | IllegalArgumentException exception) {
            throw invalidResponse(operation);
        }
    }

    private GoogleUserInfo toUserInfo(GoogleUserInfoPayload payload, String operation) {
        if (payload.subject() == null || payload.subject().isBlank()
                || payload.email() == null || payload.email().isBlank()) {
            throw invalidResponse(operation);
        }

        try {
            return new GoogleUserInfo(
                    payload.subject(),
                    payload.email(),
                    isEmailVerified(payload.emailVerified()));
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(operation);
        }
    }

    private GoogleOAuthException httpFailure(String operation, ConvertibleClientHttpResponse response) {
        int status;
        try {
            status = response.getStatusCode().value();
        } catch (IOException exception) {
            return networkFailure(operation);
        }
        GoogleErrorPayload errorPayload;
        try {
            errorPayload = response.bodyTo(GoogleErrorPayload.class);
        } catch (RuntimeException exception) {
            return invalidResponse(operation);
        }

        GoogleOAuthErrorCode errorCode = errorPayload == null
                ? null
                : mapGoogleError(errorPayload.error());
        if (errorCode == null) {
            errorCode = mapHttpStatus(status);
        }
        return httpFailure(operation, status, errorCode);
    }

    private GoogleOAuthException httpFailure(String operation, int status) {
        return httpFailure(operation, status, mapHttpStatus(status));
    }

    private GoogleOAuthException httpFailure(
            String operation,
            int status,
            GoogleOAuthErrorCode errorCode) {
        return new GoogleOAuthException(
                errorCode,
                operation + " failed (HTTP " + status + ", " + errorCode.name() + ")");
    }

    private GoogleOAuthException networkFailure(String operation) {
        return new GoogleOAuthException(
                GoogleOAuthErrorCode.NETWORK_ERROR,
                operation + " failed (" + GoogleOAuthErrorCode.NETWORK_ERROR.name() + ")");
    }

    private GoogleOAuthException invalidResponse(String operation) {
        return new GoogleOAuthException(
                GoogleOAuthErrorCode.INVALID_RESPONSE,
                operation + " failed (" + GoogleOAuthErrorCode.INVALID_RESPONSE.name() + ")");
    }

    private GoogleOAuthErrorCode mapGoogleError(String error) {
        if (error == null) {
            return null;
        }
        return switch (error) {
            case "invalid_grant" -> GoogleOAuthErrorCode.INVALID_GRANT;
            case "invalid_request" -> GoogleOAuthErrorCode.INVALID_REQUEST;
            case "invalid_client" -> GoogleOAuthErrorCode.INVALID_CLIENT;
            case "invalid_token" -> GoogleOAuthErrorCode.INVALID_TOKEN;
            default -> null;
        };
    }

    private GoogleOAuthErrorCode mapHttpStatus(int status) {
        if (status == 401) {
            return GoogleOAuthErrorCode.UNAUTHORIZED;
        }
        if (status == 429) {
            return GoogleOAuthErrorCode.RATE_LIMITED;
        }
        if (status >= 500 && status <= 599) {
            return GoogleOAuthErrorCode.UPSTREAM_ERROR;
        }
        return GoogleOAuthErrorCode.UNKNOWN;
    }

    private List<String> parseScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return List.of();
        }
        return Arrays.stream(scope.trim().split("\\s+"))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private boolean isEmailVerified(Object emailVerified) {
        return Boolean.TRUE.equals(emailVerified) || "true".equals(emailVerified);
    }

    private MultiValueMap<String, String> formData(String... keyValues) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            form.add(keyValues[index], keyValues[index + 1]);
        }
        return form;
    }

    private void requireAuthorizationConfiguration() {
        if (properties.clientId() == null || properties.clientId().isBlank()
                || properties.redirectUri() == null || properties.redirectUri().isBlank()) {
            throw new IllegalArgumentException("OAuth client configuration is incomplete");
        }
    }

    private void requireTokenConfiguration() {
        requireAuthorizationConfiguration();
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new IllegalArgumentException("OAuth client configuration is incomplete");
        }
    }

    private void requireScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("OAuth scopes are required");
        }
        for (String scope : scopes) {
            if (scope == null || scope.isBlank()) {
                throw new IllegalArgumentException("OAuth scopes must not contain blank values");
            }
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenPayload(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("scope") String scope,
            @JsonProperty("token_type") String tokenType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleUserInfoPayload(
            @JsonProperty("sub") String subject,
            @JsonProperty("email") String email,
            @JsonProperty("email_verified") Object emailVerified) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleErrorPayload(@JsonProperty("error") String error) {
    }
}