package com.fluxisai.proposal;
import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProposalRepository extends JpaRepository<Proposal,UUID>{Optional<Proposal> findByIdAndUserId(UUID id,UUID userId);Page<Proposal> findAllByUserId(UUID userId,Pageable pageable);}
