package com.fluxisai.common.security;

import java.util.UUID;

import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevCurrentUser implements CurrentUser {

    private static final String DEV_EMAIL = "dev@fluxis.local";

    private final UserRepository userRepository;
    private volatile UUID cachedId;

    public DevCurrentUser(UserRepository userRepository) {
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
                cachedId = userRepository.findByEmailIgnoreCase(DEV_EMAIL)
                        .map(User::getId)
                        .orElseGet(() -> userRepository.save(new User(DEV_EMAIL, "Development User")).getId());
            }
            return cachedId;
        }
    }
}
