package com.fluxisai.catalog;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalServiceRepository extends JpaRepository<ProfessionalService,UUID>{
    boolean existsByUserIdAndNameIgnoreCase(UUID userId,String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId,String name,UUID id);
    Optional<ProfessionalService> findByIdAndUserId(UUID id,UUID userId);
    Page<ProfessionalService> findAllByUserId(UUID userId,Pageable pageable);
}
