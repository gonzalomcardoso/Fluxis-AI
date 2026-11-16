package com.fluxisai.application;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateJobApplicationRequest(
        @NotNull UUID jobOpportunityId,
        ApplicationStatus status,
        LocalDate appliedOn,
        @Size(max = 20000) String notes) {
}
