package com.fluxisai.proposal;
import java.math.BigDecimal; import java.util.UUID;
public record ProposalItemResponse(UUID id,UUID serviceId,String description,BigDecimal quantity,BigDecimal unitPrice,BigDecimal amount){static ProposalItemResponse from(ProposalItem i){return new ProposalItemResponse(i.getId(),i.getServiceId(),i.getDescription(),i.getQuantity(),i.getUnitPrice(),i.getAmount());}}
