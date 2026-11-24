package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class TokenEncryptionServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String ASSOCIATED_DATA = "fluxis:gmail-oauth:user:google-sub";

    @Test
    void encryptsAndDecryptsOAuthToken() {
        TokenEncryptionService service = new TokenEncryptionService(KEY, 1);

        String ciphertext = service.encrypt("plain-access-token", ASSOCIATED_DATA);

        assertThat(ciphertext).startsWith("v1:").doesNotContain("plain-access-token");
        assertThat(service.decrypt(ciphertext, 1, ASSOCIATED_DATA)).isEqualTo("plain-access-token");
    }

    @Test
    void usesRandomInitializationVectors() {
        TokenEncryptionService service = new TokenEncryptionService(KEY, 1);

        String first = service.encrypt("same-token", ASSOCIATED_DATA);
        String second = service.encrypt("same-token", ASSOCIATED_DATA);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void bindsCiphertextToItsAssociatedData() {
        TokenEncryptionService service = new TokenEncryptionService(KEY, 1);
        String ciphertext = service.encrypt("plain-access-token", ASSOCIATED_DATA);

        assertThatThrownBy(() -> service.decrypt(ciphertext, 1, "different-context"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void failsClosedWithoutAKey() {
        TokenEncryptionService service = new TokenEncryptionService("", 1);

        assertThat(service.isConfigured()).isFalse();
        assertThatThrownBy(() -> service.encrypt("token", ASSOCIATED_DATA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_TOKEN_ENCRYPTION_KEY");
    }

    @Test
    void rejectsInvalidKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new TokenEncryptionService(shortKey, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32-byte");
    }

    @Test
    void rejectsUnsupportedKeyVersion() {
        TokenEncryptionService service = new TokenEncryptionService(KEY, 1);
        String ciphertext = service.encrypt("token", ASSOCIATED_DATA);

        assertThatThrownBy(() -> service.decrypt(ciphertext, 2, ASSOCIATED_DATA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key version");
    }
}
