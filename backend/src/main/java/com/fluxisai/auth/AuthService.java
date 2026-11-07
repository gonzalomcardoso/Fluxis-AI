package com.fluxisai.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthenticatedUserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe una cuenta con ese correo");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("La contraseña no puede superar 72 bytes UTF-8");
        }

        User user = new User(email, request.displayName().trim(), passwordEncoder.encode(request.password()));
        try {
            return AuthenticatedUserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Ya existe una cuenta con ese correo");
        }
    }
}
