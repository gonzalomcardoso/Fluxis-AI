package com.fluxisai.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    boolean existsByUserIdAndJobOpportunityId(UUID userId, UUID jobOpportunityId);
    Optional<JobApplication> findByIdAndUserId(UUID id, UUID userId);
    Page<JobApplication> findAllByUserId(UUID userId, Pageable pageable);
}
