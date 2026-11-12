package com.fluxisai.application;

import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationFollowUpRepository extends JpaRepository<ApplicationFollowUp, UUID> {
    List<ApplicationFollowUp> findAllByJobApplicationIdAndUserId(UUID jobApplicationId, UUID userId, Sort sort);
    Optional<ApplicationFollowUp> findByIdAndJobApplicationIdAndUserId(UUID id, UUID jobApplicationId, UUID userId);
}
