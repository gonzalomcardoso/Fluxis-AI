package com.fluxisai.auth;

import java.util.Collection;
import java.util.UUID;

import com.fluxisai.user.User;
import com.fluxisai.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record FluxisUserDetails(UUID id, String email, String displayName, String passwordHash, UserStatus status)
        implements UserDetails {

    public static FluxisUserDetails from(User user) {
        return new FluxisUserDetails(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPasswordHash(),
                user.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
