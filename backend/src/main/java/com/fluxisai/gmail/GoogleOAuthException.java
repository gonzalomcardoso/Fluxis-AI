package com.fluxisai.gmail;

/**
 * Signals a failure while interacting with Google's OAuth services.
 *
 * <p>Messages must remain safe for logs and user-facing error handling: they
 * must not contain tokens, secrets, OAuth codes, or HTTP response bodies. Causes
 * must not contain sensitive values either.</p>
 */
public class GoogleOAuthException extends RuntimeException {

    private final GoogleOAuthErrorCode errorCode;

    public GoogleOAuthException(String message) {
        super(message);
        this.errorCode = GoogleOAuthErrorCode.UNKNOWN;
    }

    public GoogleOAuthException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = GoogleOAuthErrorCode.UNKNOWN;
    }

    public GoogleOAuthException(GoogleOAuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GoogleOAuthErrorCode getErrorCode() {
        return errorCode;
    }
}