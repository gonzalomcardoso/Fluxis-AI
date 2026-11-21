package com.fluxisai.application;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewRequest(
        @NotNull OffsetDateTime scheduledAt,
        @NotBlank @Size(max = 100) String interviewType,
        @Size(max = 20000) String notes) {
}
