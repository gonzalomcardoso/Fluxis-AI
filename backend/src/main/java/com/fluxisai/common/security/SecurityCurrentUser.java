package com.fluxisai.common.security;

import java.util.UUID;

import com.fluxisai.auth.FluxisUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUser implements CurrentUser {

    @Override
    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof FluxisUserDetails user)) {
            throw new IllegalStateException("An authenticated Fluxis user is required");
        }
        return user.id();
    }
}
