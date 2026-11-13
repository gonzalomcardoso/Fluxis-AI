package com.fluxisai.catalog;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfessionalServiceResponse(UUID id,String name,String description,String category,PricingModel pricingModel,BigDecimal rate,String currency,boolean available,ProfessionalServiceStatus status,String notes,OffsetDateTime createdAt,OffsetDateTime updatedAt){
    public static ProfessionalServiceResponse from(ProfessionalService s){return new ProfessionalServiceResponse(s.getId(),s.getName(),s.getDescription(),s.getCategory(),s.getPricingModel(),s.getRate(),s.getCurrency(),s.isAvailable(),s.getStatus(),s.getNotes(),s.getCreatedAt(),s.getUpdatedAt());}
}
