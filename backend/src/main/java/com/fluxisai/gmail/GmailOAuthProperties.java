package com.fluxisai.gmail;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gmail.oauth")
public record GmailOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        List<String> scopes) {

    public GmailOAuthProperties {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }
}
