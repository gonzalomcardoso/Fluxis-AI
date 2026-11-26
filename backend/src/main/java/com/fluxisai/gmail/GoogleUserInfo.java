package com.fluxisai.gmail;

/**
 * Represents the identity returned by Google's UserInfo endpoint.
 *
 * <p>The stable account identifier is the subject. Email verification is
 * intentionally validated by a later component.</p>
 */
public record GoogleUserInfo(
        String subject,
        String email,
        boolean emailVerified) {

    public GoogleUserInfo {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Google subject is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google email is required");
        }
    }

    /**
     * Returns a safe representation that never includes identity values.
     */
    @Override
    public String toString() {
        return "GoogleUserInfo[emailVerified=%s]".formatted(emailVerified);
    }
}