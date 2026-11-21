package com.fluxisai.proposal;
import java.util.*; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProposalHistoryRepository extends JpaRepository<ProposalHistory,UUID>{List<ProposalHistory> findAllByProposalIdAndUserIdOrderByChangedAtDesc(UUID proposalId,UUID userId);}
