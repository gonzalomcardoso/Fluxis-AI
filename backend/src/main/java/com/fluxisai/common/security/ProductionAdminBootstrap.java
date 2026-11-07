package com.fluxisai.common.security;

import java.io.Console;
import java.util.Arrays;
import java.util.Set;

import com.fluxisai.auth.AuthService;
import com.fluxisai.auth.RegisterRequest;
import com.fluxisai.user.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod-bootstrap")
public class ProductionAdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final Validator validator;

    public ProductionAdminBootstrap(
            UserRepository userRepository,
            AuthService authService,
            Validator validator) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.validator = validator;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() != 0) {
            throw new IllegalStateException("Admin bootstrap requires an empty users table.");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run the admin bootstrap from an interactive console.");
        }

        String email = console.readLine("Admin email: ");
        String displayName = console.readLine("Admin display name: ");
        if (email == null || displayName == null) {
            throw new IllegalStateException("Admin bootstrap input ended unexpectedly.");
        }

        char[] password = console.readPassword("Admin password (minimum 12 characters): ");
        char[] confirmation = console.readPassword("Confirm admin password: ");
        if (password == null || confirmation == null) {
            clear(password, confirmation);
            throw new IllegalStateException("Admin bootstrap password input ended unexpectedly.");
        }

        try {
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("The admin passwords do not match.");
            }

            RegisterRequest request = new RegisterRequest(
                    email.trim(),
                    displayName.trim(),
                    new String(password));
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .sorted()
                        .reduce((first, second) -> first + "; " + second)
                        .orElse("Invalid admin details.");
                throw new IllegalArgumentException("Invalid admin details: " + errors);
            }

            authService.register(request);
            console.printf("The single application account was created for %s.%n", request.email());
        } finally {
            clear(password, confirmation);
        }
    }

    private static void clear(char[] password, char[] confirmation) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
        if (confirmation != null) {
            Arrays.fill(confirmation, '\0');
        }
    }
}
