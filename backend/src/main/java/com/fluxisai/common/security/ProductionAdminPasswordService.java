package com.fluxisai.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.fluxisai.auth.RegisterRequest;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionAdminPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public ProductionAdminPasswordService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Validator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public void updatePassword(User user, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("The password cannot exceed 72 UTF-8 bytes.");
        }

        RegisterRequest validationRequest = new RegisterRequest(
                user.getEmail(),
                user.getDisplayName(),
                password);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(validationRequest);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted()
                    .reduce((first, second) -> first + "; " + second)
                    .orElse("Invalid password.");
            throw new IllegalArgumentException("Invalid password: " + errors);
        }

        user.updateCredentials(passwordEncoder.encode(password), user.getStatus());
        userRepository.saveAndFlush(user);
    }
}
