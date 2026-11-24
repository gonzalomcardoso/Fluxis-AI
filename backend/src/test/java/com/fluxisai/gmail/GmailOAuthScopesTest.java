package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GmailOAuthScopesTest {

    @Test
    void baselineContainsOnlyIdentityAndMetadataScopes() {
        assertThat(GmailOAuthScopes.BASELINE)
                .containsExactly(
                        GmailOAuthScopes.OPENID,
                        GmailOAuthScopes.EMAIL,
                        GmailOAuthScopes.GMAIL_METADATA)
                .doesNotContain(
                        GmailOAuthScopes.GMAIL_MODIFY,
                        GmailOAuthScopes.GMAIL_COMPOSE,
                        "https://www.googleapis.com/auth/gmail.send",
                        "https://www.googleapis.com/auth/gmail.readonly",
                        "https://mail.google.com/");
    }
}
