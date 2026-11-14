package com.fluxisai.proposal;
import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.UUID;
public record ProposalHistoryResponse(UUID id,String changeType,ProposalStatus status,BigDecimal subtotal,BigDecimal total,OffsetDateTime changedAt){static ProposalHistoryResponse from(ProposalHistory h){return new ProposalHistoryResponse(h.getId(),h.getChangeType(),h.getStatus(),h.getSubtotal(),h.getTotal(),h.getChangedAt());}}
