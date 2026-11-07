package com.fluxisai.common.security;

import java.io.Console;
import java.util.Arrays;

import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod-password-reset")
public class ProductionAdminPasswordReset implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductionAdminPasswordService passwordService;

    public ProductionAdminPasswordReset(
            UserRepository userRepository,
            ProductionAdminPasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() != 1) {
            throw new IllegalStateException("Password reset requires exactly one application account.");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run the password reset from an interactive console.");
        }

        String email = console.readLine("Existing account email: ");
        if (email == null) {
            throw new IllegalStateException("Password reset input ended unexpectedly.");
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("The account email does not match."));
        String confirmation = console.readLine("Type RESET to continue: ");
        if (!"RESET".equals(confirmation)) {
            throw new IllegalArgumentException("Password reset was not confirmed.");
        }

        char[] password = console.readPassword("New password (minimum 12 characters): ");
        char[] repeatedPassword = console.readPassword("Confirm new password: ");
        if (password == null || repeatedPassword == null) {
            clear(password, repeatedPassword);
            throw new IllegalStateException("Password reset input ended unexpectedly.");
        }

        try {
            if (!Arrays.equals(password, repeatedPassword)) {
                throw new IllegalArgumentException("The passwords do not match.");
            }
            passwordService.updatePassword(user, new String(password));
            console.printf("Password updated for %s.%n", user.getEmail());
        } finally {
            clear(password, repeatedPassword);
        }
    }

    private static void clear(char[] password, char[] repeatedPassword) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
        if (repeatedPassword != null) {
            Arrays.fill(repeatedPassword, '\0');
        }
    }
}
