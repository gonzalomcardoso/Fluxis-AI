package com.fluxisai.gmail;

import java.net.URI;
import java.util.List;

/**
 * Client boundary for Google's OAuth HTTP endpoints.
 */
public interface GoogleOAuthClient {

    URI buildAuthorizationUri(String state, String codeChallenge, List<String> scopes);

    GoogleOAuthTokens exchangeAuthorizationCode(String code, String codeVerifier);

    GoogleOAuthTokens refreshAccessToken(String refreshToken);

    GoogleUserInfo fetchUserInfo(String accessToken);

    void revokeToken(String token);
}