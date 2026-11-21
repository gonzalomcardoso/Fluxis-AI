package com.fluxisai.application;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record UpdateJobApplicationRequest(
        ApplicationStatus status,
        LocalDate appliedOn,
        @Size(max = 20000) String notes) {
}
