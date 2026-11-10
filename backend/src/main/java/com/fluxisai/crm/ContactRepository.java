package com.fluxisai.crm;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    boolean existsByUserIdAndEmailIgnoreCase(UUID userId, String email);
    boolean existsByUserIdAndEmailIgnoreCaseAndIdNot(UUID userId, String email, UUID id);
    Optional<Contact> findByIdAndUserId(UUID id, UUID userId);
    Page<Contact> findAllByUserId(UUID userId, Pageable pageable);
}
