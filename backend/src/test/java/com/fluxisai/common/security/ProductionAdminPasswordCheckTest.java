package com.fluxisai.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductionAdminPasswordCheckTest {

    @Test
    void summarizesTwelveLowercaseLettersAsLettersAndDigits() {
        ProductionAdminPasswordCheck.PasswordComposition composition =
                ProductionAdminPasswordCheck.summarize("abcdefghijkl".toCharArray());

        assertThat(composition.length()).isEqualTo(12);
        assertThat(composition.lettersAndDigits()).isEqualTo(12);
        assertThat(composition.otherCharacters()).isZero();
    }

    @Test
    void countsNonAlphanumericCodePointsSeparately() {
        ProductionAdminPasswordCheck.PasswordComposition composition =
                ProductionAdminPasswordCheck.summarize("ab12é!".toCharArray());

        assertThat(composition.length()).isEqualTo(6);
        assertThat(composition.lettersAndDigits()).isEqualTo(5);
        assertThat(composition.otherCharacters()).isEqualTo(1);
    }
}
