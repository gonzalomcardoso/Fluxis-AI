package com.fluxisai.auth;

import java.util.UUID;

import com.fluxisai.user.User;

public record AuthenticatedUserResponse(UUID id, String email, String displayName) {

    static AuthenticatedUserResponse from(User user) {
        return new AuthenticatedUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    static AuthenticatedUserResponse from(FluxisUserDetails user) {
        return new AuthenticatedUserResponse(user.id(), user.getUsername(), user.displayName());
    }
}
