package com.fluxisai.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobApplicationResponse(
        UUID id,
        UUID jobOpportunityId,
        String companyName,
        String title,
        ApplicationStatus status,
        LocalDate appliedOn,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
