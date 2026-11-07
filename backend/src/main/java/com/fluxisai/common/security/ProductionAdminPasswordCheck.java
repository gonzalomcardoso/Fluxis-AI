package com.fluxisai.common.security;

import java.io.Console;
import java.util.Arrays;

import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("prod-password-check")
public class ProductionAdminPasswordCheck implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProductionAdminPasswordCheck(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() != 1) {
            throw new IllegalStateException("Password check requires exactly one application account.");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run the password check from an interactive console.");
        }

        String email = console.readLine("Existing account email: ");
        if (email == null) {
            throw new IllegalStateException("Password check input ended unexpectedly.");
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("The account email does not match."));
        char[] password = console.readPassword("Password: ");
        if (password == null) {
            throw new IllegalStateException("Password check input ended unexpectedly.");
        }

        try {
            PasswordComposition composition = summarize(password);
            boolean matches = passwordEncoder.matches(new String(password), user.getPasswordHash());
            console.printf("Length received: %d%n", composition.length());
            console.printf("Letters/digits: %d, other characters: %d%n",
                    composition.lettersAndDigits(), composition.otherCharacters());
            console.printf("Result: %s%n", matches ? "MATCH" : "NO MATCH");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    static PasswordComposition summarize(char[] password) {
        int length = 0;
        int lettersAndDigits = 0;
        for (int offset = 0; offset < password.length;) {
            int codePoint = Character.codePointAt(password, offset);
            offset += Character.charCount(codePoint);
            length++;
            if (Character.isLetterOrDigit(codePoint)) {
                lettersAndDigits++;
            }
        }
        return new PasswordComposition(length, lettersAndDigits, length - lettersAndDigits);
    }

    record PasswordComposition(int length, int lettersAndDigits, int otherCharacters) {
    }
}
