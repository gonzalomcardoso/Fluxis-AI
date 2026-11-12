package com.fluxisai.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FollowUpResponse(UUID id, UUID jobApplicationId, LocalDate dueOn, String description, boolean completed, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static FollowUpResponse from(ApplicationFollowUp followUp) { return new FollowUpResponse(followUp.getId(), followUp.getJobApplicationId(), followUp.getDueOn(), followUp.getDescription(), followUp.isCompleted(), followUp.getCreatedAt(), followUp.getUpdatedAt()); }
}
