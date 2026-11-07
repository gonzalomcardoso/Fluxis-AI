package com.fluxisai.job;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JobOpportunityResponse(
        UUID id,
        String companyName,
        String title,
        String url,
        String source,
        LocalDate postedOn,
        String description,
        String requirements,
        List<String> technologies,
        JobStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static JobOpportunityResponse from(JobOpportunity opportunity) {
        return new JobOpportunityResponse(
                opportunity.getId(),
                opportunity.getCompanyName(),
                opportunity.getTitle(),
                opportunity.getUrl(),
                opportunity.getSource(),
                opportunity.getPostedOn(),
                opportunity.getDescription(),
                opportunity.getRequirements(),
                opportunity.getTechnologies(),
                opportunity.getStatus(),
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt());
    }
}
