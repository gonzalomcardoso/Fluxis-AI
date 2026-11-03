package com.fluxisai.job;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobOpportunityRepository extends JpaRepository<JobOpportunity, UUID> {

    boolean existsByUserIdAndUrl(UUID userId, String url);

    Optional<JobOpportunity> findByIdAndUserId(UUID id, UUID userId);

    Page<JobOpportunity> findAllByUserId(UUID userId, Pageable pageable);
}
