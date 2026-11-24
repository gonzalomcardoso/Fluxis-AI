package com.fluxisai.gmail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, UUID> {

    Optional<ConnectedAccount> findByUserIdAndProvider(UUID userId, ConnectedAccountProvider provider);
}
