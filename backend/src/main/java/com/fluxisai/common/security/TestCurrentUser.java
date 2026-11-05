package com.fluxisai.common.security;

import java.util.UUID;

import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestCurrentUser implements CurrentUser {

    private static final String TEST_EMAIL = "integration@fluxis.local";

    private final UserRepository userRepository;
    private volatile UUID cachedId;

    public TestCurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UUID id() {
        UUID userId = cachedId;
        if (userId != null) {
            return userId;
        }

        synchronized (this) {
            if (cachedId == null) {
                cachedId = userRepository.findByEmailIgnoreCase(TEST_EMAIL)
                        .map(User::getId)
                        .orElseGet(() -> userRepository.save(
                                new User(TEST_EMAIL, "Synthetic Integration User")).getId());
            }
            return cachedId;
        }
    }
}
