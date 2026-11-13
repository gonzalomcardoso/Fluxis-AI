package com.fluxisai.proposal;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProposalItemRepository extends JpaRepository<ProposalItem,UUID>{List<ProposalItem> findAllByProposalId(UUID proposalId);void deleteAllByProposalId(UUID proposalId);}
