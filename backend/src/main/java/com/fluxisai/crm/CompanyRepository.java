package com.fluxisai.crm;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);
    Optional<Company> findByIdAndUserId(UUID id, UUID userId);
    Page<Company> findAllByUserId(UUID userId, Pageable pageable);
}
