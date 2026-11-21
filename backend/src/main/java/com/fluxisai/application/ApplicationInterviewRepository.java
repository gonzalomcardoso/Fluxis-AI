package com.fluxisai.application;

import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationInterviewRepository extends JpaRepository<ApplicationInterview, UUID> {
    List<ApplicationInterview> findAllByJobApplicationIdAndUserId(UUID jobApplicationId, UUID userId, Sort sort);
    Optional<ApplicationInterview> findByIdAndJobApplicationIdAndUserId(UUID id, UUID jobApplicationId, UUID userId);
}
