package com.fluxisai.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GoogleUserInfoTest {

    @Test
    void toStringDoesNotExposeIdentityWhenEmailIsVerified() {
        GoogleUserInfo userInfo = new GoogleUserInfo(
                "GOOGLE-SUBJECT-SECRET-123",
                "SECRET-EMAIL@example.test",
                true);

        String result = userInfo.toString();

        assertThat(result)
                .doesNotContain("GOOGLE-SUBJECT-SECRET-123", "SECRET-EMAIL@example.test")
                .contains("emailVerified=true");
    }

    @Test
    void toStringDoesNotExposeIdentityWhenEmailIsNotVerified() {
        GoogleUserInfo userInfo = new GoogleUserInfo(
                "GOOGLE-SUBJECT-SECRET-123",
                "SECRET-EMAIL@example.test",
                false);

        String result = userInfo.toString();

        assertThat(result)
                .doesNotContain("GOOGLE-SUBJECT-SECRET-123", "SECRET-EMAIL@example.test")
                .contains("emailVerified=false");
    }

    @Test
    void rejectsNullOrBlankSubjectWithLiteralSafeMessage() {
        assertThatThrownBy(() -> new GoogleUserInfo(null, "person@example.test", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google subject is required");

        assertThatThrownBy(() -> new GoogleUserInfo("", "person@example.test", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google subject is required");

        assertThatThrownBy(() -> new GoogleUserInfo("   ", "person@example.test", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google subject is required");
    }

    @Test
    void rejectsNullOrBlankEmailWithLiteralSafeMessage() {
        assertThatThrownBy(() -> new GoogleUserInfo("google-subject-123", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google email is required");

        assertThatThrownBy(() -> new GoogleUserInfo("google-subject-123", "", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google email is required");

        assertThatThrownBy(() -> new GoogleUserInfo("google-subject-123", "   ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google email is required");
    }

    @Test
    void exposesValidatedValuesThroughAccessors() {
        GoogleUserInfo userInfo = new GoogleUserInfo(
                "google-subject-123",
                "person@example.test",
                true);

        assertThat(userInfo.subject()).isEqualTo("google-subject-123");
        assertThat(userInfo.email()).isEqualTo("person@example.test");
        assertThat(userInfo.emailVerified()).isTrue();
    }
}