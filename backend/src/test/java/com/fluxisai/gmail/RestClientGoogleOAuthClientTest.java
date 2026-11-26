package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class RestClientGoogleOAuthClientTest {

    private static final String AUTHORIZATION_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String REVOCATION_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "SENTINEL-SECRET";
    private static final String REDIRECT_URI =
            "https://fluxis.example.test/oauth/callback path?source=test&mode=oauth";
    private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");
    private static final String[] SENTINELS = {
            "SENTINEL-ACCESS",
            "SENTINEL-CODE",
            "SENTINEL-SECRET",
            "SENTINEL-VERIFIER",
            "SENTINEL-REFRESH",
            "SENTINEL-REVOKE"
    };

    private MockRestServiceServer server;
    private RestClientGoogleOAuthClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        GmailOAuthProperties properties = new GmailOAuthProperties(
                CLIENT_ID,
                CLIENT_SECRET,
                REDIRECT_URI,
                List.of("configured-scope-a", "configured-scope-b"));
        client = new RestClientGoogleOAuthClient(
                builder.build(),
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void buildsAuthorizationUriWithExpectedDecodedParameters() {
        URI uri = client.buildAuthorizationUri(
                "state value/with?special&chars",
                "challenge-value_123",
                List.of("scope-a", "scope-b"));

        Map<String, String> parameters = queryParameters(uri);

        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("accounts.google.com");
        assertThat(uri.getPath()).isEqualTo("/o/oauth2/v2/auth");
        assertThat(parameters)
                .containsEntry("client_id", CLIENT_ID)
                .containsEntry("redirect_uri", REDIRECT_URI)
                .containsEntry("response_type", "code")
                .containsEntry("scope", "scope-a scope-b")
                .containsEntry("state", "state value/with?special&chars")
                .containsEntry("code_challenge", "challenge-value_123")
                .containsEntry("code_challenge_method", "S256")
                .containsEntry("access_type", "offline")
                .containsEntry("prompt", "consent")
                .doesNotContainKeys("client_secret", "include_granted_scopes");
    }

    @Test
    void rejectsNullOrBlankAuthorizationParameters() {
        assertThatThrownBy(() -> client.buildAuthorizationUri(
                null, "challenge", List.of("scope-a")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAuth state is required");
        assertThatThrownBy(() -> client.buildAuthorizationUri(
                "  ", "challenge", List.of("scope-a")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAuth state is required");
        assertThatThrownBy(() -> client.buildAuthorizationUri(
                "state", null, List.of("scope-a")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PKCE code challenge is required");
        assertThatThrownBy(() -> client.buildAuthorizationUri(
                "state", "   ", List.of("scope-a")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PKCE code challenge is required");
        assertThatThrownBy(() -> client.buildAuthorizationUri("state", "challenge", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAuth scopes are required");
        assertThatThrownBy(() -> client.buildAuthorizationUri("state", "challenge", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAuth scopes are required");
        assertThatThrownBy(() -> client.buildAuthorizationUri(
                "state", "challenge", List.of("scope-a", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAuth scopes must not contain blank values");
    }

    @Test
    void rejectsNullOrBlankOperationParameters() {
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(null, "verifier"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authorization code is required");
        assertThatThrownBy(() -> client.exchangeAuthorizationCode("code", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PKCE code verifier is required");
        assertThatThrownBy(() -> client.refreshAccessToken(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token is required");
        assertThatThrownBy(() -> client.fetchUserInfo(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access token is required");
        assertThatThrownBy(() -> client.revokeToken(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token to revoke is required");
        assertThatThrownBy(() -> client.exchangeAuthorizationCode("   ", "verifier"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authorization code is required");
        assertThatThrownBy(() -> client.exchangeAuthorizationCode("code", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PKCE code verifier is required");
        assertThatThrownBy(() -> client.refreshAccessToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token is required");
        assertThatThrownBy(() -> client.fetchUserInfo("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access token is required");
        assertThatThrownBy(() -> client.revokeToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token to revoke is required");
    }

    @Test
    void exchangesAuthorizationCodeWithExactFormAndParsesTokenResponse() {
        MultiValueMap<String, String> expectedForm = form(
                "code", "auth-code-123",
                "client_id", CLIENT_ID,
                "client_secret", CLIENT_SECRET,
                "redirect_uri", REDIRECT_URI,
                "grant_type", "authorization_code",
                "code_verifier", "verifier-123");
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-123",
                          "refresh_token": "refresh-456",
                          "expires_in": 3600,
                          "scope": "scope-a scope-b",
                          "token_type": "Bearer",
                          "id_token": "ignored"
                        }
                        """, MediaType.APPLICATION_JSON));

        GoogleOAuthTokens tokens = client.exchangeAuthorizationCode("auth-code-123", "verifier-123");

        assertThat(tokens.accessToken()).isEqualTo("access-123");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-456");
        assertThat(tokens.expiresAt()).isEqualTo(OffsetDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC));
        assertThat(tokens.scopes()).containsExactly("scope-a", "scope-b");
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void exchangeWithoutOptionalTokenFieldsProducesNullsAndEmptyScopes() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token": "access-only"}
                        """, MediaType.APPLICATION_JSON));

        GoogleOAuthTokens tokens = client.exchangeAuthorizationCode("auth-code-123", "verifier-123");

        assertThat(tokens.accessToken()).isEqualTo("access-only");
        assertThat(tokens.refreshToken()).isNull();
        assertThat(tokens.expiresAt()).isNull();
        assertThat(tokens.scopes()).isEmpty();
        assertThat(tokens.tokenType()).isNull();
    }

    @Test
    void handlesZeroNegativeAndHugeExpiresInValues() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"zero-access","expires_in":0}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"SENTINEL-ACCESS","expires_in":-1}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"SENTINEL-ACCESS","expires_in":9223372036854775807}
                        """, MediaType.APPLICATION_JSON));

        GoogleOAuthTokens zero = client.exchangeAuthorizationCode("auth-code-123", "verifier-123");
        GoogleOAuthException negative = capture(() ->
                client.exchangeAuthorizationCode("auth-code-123", "verifier-123"));
        GoogleOAuthException huge = capture(() ->
                client.exchangeAuthorizationCode("auth-code-123", "verifier-123"));

        assertThat(zero.expiresAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(negative.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_RESPONSE);
        assertThat(huge.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_RESPONSE);
        assertThat(negative.getCause()).isNull();
        assertThat(huge.getCause()).isNull();
        assertThat(negative.getMessage()).doesNotContain("SENTINEL-ACCESS", "-1");
        assertThat(huge.getMessage()).doesNotContain("SENTINEL-ACCESS", String.valueOf(Long.MAX_VALUE));
        assertThat(negative.toString()).doesNotContain("SENTINEL-ACCESS");
        assertThat(huge.toString()).doesNotContain("SENTINEL-ACCESS");
    }

    @Test
    void missingAccessTokenInTokenResponseProducesInvalidResponse() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"refresh_token": "refresh-only"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeAuthorizationCode("auth-code-123", "verifier-123"))
                .isInstanceOf(GoogleOAuthException.class)
                .satisfies(exception -> {
                    GoogleOAuthException oauthException = (GoogleOAuthException) exception;
                    assertThat(oauthException.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_RESPONSE);
                    assertThat(oauthException.getCause()).isNull();
                });
    }

    @Test
    void refreshesAccessTokenWithExactForm() {
        MultiValueMap<String, String> expectedForm = form(
                "grant_type", "refresh_token",
                "refresh_token", "refresh-123",
                "client_id", CLIENT_ID,
                "client_secret", CLIENT_SECRET);
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("""
                        {"access_token": "refreshed-access"}
                        """, MediaType.APPLICATION_JSON));

        GoogleOAuthTokens tokens = client.refreshAccessToken("refresh-123");

        assertThat(tokens.accessToken()).isEqualTo("refreshed-access");
        assertThat(tokens.refreshToken()).isNull();
    }

    @Test
    void refreshInvalidGrantMapsToInvalidGrant() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"invalid_grant","error_description":"ignored"}
                                """));

        assertThatThrownBy(() -> client.refreshAccessToken("refresh-123"))
                .isInstanceOf(GoogleOAuthException.class)
                .satisfies(exception -> assertThat(((GoogleOAuthException) exception).getErrorCode())
                        .isEqualTo(GoogleOAuthErrorCode.INVALID_GRANT));
    }

    @Test
    void mapsInvalidRequestAndInvalidClientOAuthErrors() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"invalid_request"}
                                """));
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"invalid_client"}
                                """));

        GoogleOAuthException invalidRequest = capture(() ->
                client.exchangeAuthorizationCode("auth-code-123", "verifier-123"));
        GoogleOAuthException invalidClient = capture(() ->
                client.exchangeAuthorizationCode("auth-code-123", "verifier-123"));

        assertThat(invalidRequest.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_REQUEST);
        assertThat(invalidClient.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_CLIENT);
    }

    @Test
    void mapsTooManyRequestsToRateLimited() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        GoogleOAuthException exception = capture(() ->
                client.exchangeAuthorizationCode("auth-code-123", "verifier-123"));

        assertThat(exception.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.RATE_LIMITED);
    }

    @Test
    void fetchesUserInfoWithBearerAuthAndBooleanTrue() {
        server.expect(requestTo(USERINFO_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token-123"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-sub-123",
                          "email": "person@example.test",
                          "email_verified": true,
                          "ignored": "value"
                        }
                        """, MediaType.APPLICATION_JSON));

        GoogleUserInfo userInfo = client.fetchUserInfo("access-token-123");

        assertThat(userInfo.subject()).isEqualTo("google-sub-123");
        assertThat(userInfo.email()).isEqualTo("person@example.test");
        assertThat(userInfo.emailVerified()).isTrue();
    }

    @Test
    void parsesBooleanFalseStringTrueAndMissingEmailVerified() {
        server.expect(requestTo(USERINFO_ENDPOINT))
                .andRespond(withSuccess("""
                        {"sub":"sub-1","email":"one@example.test","email_verified":false}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERINFO_ENDPOINT))
                .andRespond(withSuccess("""
                        {"sub":"sub-2","email":"two@example.test","email_verified":"true"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERINFO_ENDPOINT))
                .andRespond(withSuccess("""
                        {"sub":"sub-3","email":"three@example.test"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.fetchUserInfo("token-1").emailVerified()).isFalse();
        assertThat(client.fetchUserInfo("token-2").emailVerified()).isTrue();
        assertThat(client.fetchUserInfo("token-3").emailVerified()).isFalse();
    }

    @Test
    void missingUserInfoSubjectProducesInvalidResponse() {
        server.expect(requestTo(USERINFO_ENDPOINT))
                .andRespond(withSuccess("""
                        {"email":"person@example.test","email_verified":true}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchUserInfo("access-token-123"))
                .isInstanceOf(GoogleOAuthException.class)
                .satisfies(exception -> assertThat(((GoogleOAuthException) exception).getErrorCode())
                        .isEqualTo(GoogleOAuthErrorCode.INVALID_RESPONSE));
    }

    @Test
    void missingUserInfoEmailProducesInvalidResponse() {
        server.expect(requestTo(USERINFO_ENDPOINT))
                .andRespond(withSuccess("""
                        {"sub":"google-sub-123","email_verified":true}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchUserInfo("access-token-123"))
                .isInstanceOf(GoogleOAuthException.class)
                .satisfies(exception -> assertThat(((GoogleOAuthException) exception).getErrorCode())
                        .isEqualTo(GoogleOAuthErrorCode.INVALID_RESPONSE));
    }

    @Test
    void userInfoUnauthorizedMapsToUnauthorized() {
        server.expect(requestTo(USERINFO_ENDPOINT))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        assertThatThrownBy(() -> client.fetchUserInfo("access-token-123"))
                .isInstanceOf(GoogleOAuthException.class)
                .satisfies(exception -> assertThat(((GoogleOAuthException) exception).getErrorCode())
                        .isEqualTo(GoogleOAuthErrorCode.UNAUTHORIZED));
    }

    @Test
    void revokesTokenWithExactFormAndNoTokenInUrlQueryOrHeaders() {
        String token = "SENTINEL-REVOKE";
        MultiValueMap<String, String> expectedForm = form("token", token);
        server.expect(requestTo(REVOCATION_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andExpect(request -> {
                    assertThat(request.getURI().toString()).doesNotContain(token);
                    assertThat(request.getURI().getQuery()).isNull();
                    request.getHeaders().forEach((name, values) ->
                            assertThat(String.join(",", values)).doesNotContain(token));
                })
                .andRespond(withSuccess());

        client.revokeToken(token);
    }

    @Test
    void revocationInvalidTokenMapsToInvalidToken(CapturedOutput output) {
        String token = "SENTINEL-REVOKE";
        server.expect(requestTo(REVOCATION_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"invalid_token","error_description":"%s"}
                                """.formatted(token)));

        GoogleOAuthException exception = capture(() -> client.revokeToken(token));

        assertThat(exception.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_TOKEN);
        assertThat(exception.getCause()).isNull();
        assertThat(exception.getMessage()).doesNotContain(token);
        assertThat(exception.toString()).doesNotContain(token);
        assertThat(stackTrace(exception)).doesNotContain(token);
        assertThat(output.getOut()).doesNotContain(SENTINELS);
        assertThat(output.getErr()).doesNotContain(SENTINELS);
    }

    @Test
    void revocationTooManyRequestsMapsToRateLimited() {
        server.expect(requestTo(REVOCATION_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        GoogleOAuthException exception = capture(() -> client.revokeToken("SENTINEL-REVOKE"));

        assertThat(exception.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.RATE_LIMITED);
    }

    @Test
    void revocationServerErrorMapsToUpstreamError() {
        server.expect(requestTo(REVOCATION_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        assertThatThrownBy(() -> client.revokeToken("revoke-me"))
                .isInstanceOf(GoogleOAuthException.class)
                .satisfies(exception -> assertThat(((GoogleOAuthException) exception).getErrorCode())
                        .isEqualTo(GoogleOAuthErrorCode.UPSTREAM_ERROR));
    }

    @Test
    void securityFailuresDoNotLeakSentinelsInExceptionsOrLogs(CapturedOutput output) {
        String sentinels = String.join(" ", SENTINELS);

        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"invalid_grant","error_description":"%s"}
                                """.formatted(sentinels)));
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withException(new SocketTimeoutException(sentinels)));
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withSuccess("""
                        {"access_token":"SENTINEL-ACCESS","unterminated":
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"SENTINEL-UNKNOWN"}
                                """));

        GoogleOAuthException httpError = capture(() ->
                client.exchangeAuthorizationCode("SENTINEL-CODE", "SENTINEL-VERIFIER"));
        GoogleOAuthException networkError = capture(() ->
                client.refreshAccessToken("SENTINEL-REFRESH"));
        GoogleOAuthException malformedResponse = capture(() ->
                client.exchangeAuthorizationCode("SENTINEL-CODE", "SENTINEL-VERIFIER"));
        GoogleOAuthException unknownError = capture(() ->
                client.exchangeAuthorizationCode("SENTINEL-CODE", "SENTINEL-VERIFIER"));

        assertThat(httpError.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_GRANT);
        assertThat(networkError.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.NETWORK_ERROR);
        assertThat(networkError.getMessage()).isEqualTo("Google token refresh failed (NETWORK_ERROR)");
        assertThat(malformedResponse.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_RESPONSE);
        assertThat(unknownError.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.UNKNOWN);

        for (GoogleOAuthException exception : List.of(
                httpError, networkError, malformedResponse, unknownError)) {
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getMessage()).doesNotContain(SENTINELS);
            assertThat(exception.toString()).doesNotContain(SENTINELS);
            assertThat(stackTrace(exception)).doesNotContain(SENTINELS);
        }
        assertThat(output.getOut()).doesNotContain(SENTINELS);
        assertThat(output.getErr()).doesNotContain(SENTINELS);
    }

    private GoogleOAuthException capture(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Expected GoogleOAuthException");
        } catch (GoogleOAuthException exception) {
            return exception;
        }
    }

    private String stackTrace(Throwable exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private Map<String, String> queryParameters(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> parts.length > 1
                                ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                                : ""));
    }

    private MultiValueMap<String, String> form(String... keyValues) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            form.add(keyValues[index], keyValues[index + 1]);
        }
        return form;
    }
}