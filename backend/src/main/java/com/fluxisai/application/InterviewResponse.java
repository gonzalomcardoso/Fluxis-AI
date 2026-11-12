package com.fluxisai.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewResponse(UUID id, UUID jobApplicationId, OffsetDateTime scheduledAt, String interviewType, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static InterviewResponse from(ApplicationInterview interview) { return new InterviewResponse(interview.getId(), interview.getJobApplicationId(), interview.getScheduledAt(), interview.getInterviewType(), interview.getNotes(), interview.getCreatedAt(), interview.getUpdatedAt()); }
}
