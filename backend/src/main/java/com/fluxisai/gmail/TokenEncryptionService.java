package com.fluxisai.gmail;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts OAuth credentials before they are persisted.
 *
 * <p>The configuration key is intentionally optional at application startup so
 * the rest of Fluxis AI can run before Gmail is configured. Every encrypt or
 * decrypt operation fails closed when the key is missing.</p>
 */
@Component
public class TokenEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String CIPHERTEXT_PREFIX = "v1";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final int keyVersion;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenEncryptionService(
            @Value("${app.gmail.token-encryption-key:}") String encodedKey,
            @Value("${app.gmail.token-encryption-key-version:1}") int keyVersion) {
        if (keyVersion < 1) {
            throw new IllegalArgumentException("Token encryption key version must be positive");
        }
        this.key = parseKey(encodedKey);
        this.keyVersion = keyVersion;
    }

    public boolean isConfigured() {
        return key != null;
    }

    public int keyVersion() {
        return keyVersion;
    }

    public String encrypt(String plaintext, String associatedData) {
        requireConfigured();
        Objects.requireNonNull(plaintext, "plaintext");
        Objects.requireNonNull(associatedData, "associatedData");
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return CIPHERTEXT_PREFIX + ":" + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt the OAuth token", exception);
        }
    }

    public String decrypt(String ciphertext, int encryptedWithKeyVersion, String associatedData) {
        requireConfigured();
        Objects.requireNonNull(ciphertext, "ciphertext");
        Objects.requireNonNull(associatedData, "associatedData");
        if (encryptedWithKeyVersion != keyVersion) {
            throw new IllegalStateException("OAuth token was encrypted with an unsupported key version");
        }
        try {
            if (!ciphertext.startsWith(CIPHERTEXT_PREFIX + ":")) {
                throw new IllegalArgumentException("Unsupported OAuth token ciphertext format");
            }
            byte[] payload = Base64.getDecoder().decode(ciphertext.substring(CIPHERTEXT_PREFIX.length() + 1));
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("OAuth token ciphertext is too short");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] encrypted = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Stored OAuth token ciphertext is invalid", exception);
        }
    }

    private void requireConfigured() {
        if (key == null) {
            throw new IllegalStateException(
                    "APP_TOKEN_ENCRYPTION_KEY is not configured; OAuth tokens cannot be stored securely");
        }
    }

    private static SecretKeySpec parseKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey.trim());
            if (decoded.length != KEY_LENGTH_BYTES) {
                throw new IllegalArgumentException(
                        "APP_TOKEN_ENCRYPTION_KEY must be a base64-encoded 32-byte key");
            }
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "APP_TOKEN_ENCRYPTION_KEY must be a base64-encoded 32-byte key", exception);
        }
    }
}
