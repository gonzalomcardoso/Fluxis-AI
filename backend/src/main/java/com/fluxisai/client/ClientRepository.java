package com.fluxisai.client;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByIdAndUserId(UUID id, UUID userId);
    Page<Client> findAllByUserId(UUID userId, Pageable pageable);
}
