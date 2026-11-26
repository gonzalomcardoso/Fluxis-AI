package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleOAuthExceptionTest {

    @Test
    void messageConstructorPreservesMessageAndHasNoCause() {
        GoogleOAuthException exception = new GoogleOAuthException("Safe OAuth failure");

        assertThat(exception.getMessage()).isEqualTo("Safe OAuth failure");
        assertThat(exception.getCause()).isNull();
        assertThat(exception.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.UNKNOWN);
    }

    @Test
    void messageAndCauseConstructorPreservesBothAndUsesUnknownCode() {
        RuntimeException cause = new RuntimeException("Safe cause");
        GoogleOAuthException exception = new GoogleOAuthException("Safe OAuth failure", cause);

        assertThat(exception.getMessage()).isEqualTo("Safe OAuth failure");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.UNKNOWN);
    }

    @Test
    void errorCodeConstructorPreservesMessageAndCode() {
        GoogleOAuthException exception = new GoogleOAuthException(
                GoogleOAuthErrorCode.INVALID_GRANT,
                "Safe OAuth failure");

        assertThat(exception.getMessage()).isEqualTo("Safe OAuth failure");
        assertThat(exception.getCause()).isNull();
        assertThat(exception.getErrorCode()).isEqualTo(GoogleOAuthErrorCode.INVALID_GRANT);
    }

    @Test
    void isARuntimeException() {
        GoogleOAuthException exception = new GoogleOAuthException("Safe OAuth failure");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}