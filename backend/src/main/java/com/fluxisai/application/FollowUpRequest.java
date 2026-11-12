package com.fluxisai.application;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FollowUpRequest(
        @NotNull LocalDate dueOn,
        @NotBlank @Size(max = 500) String description,
        boolean completed) {
}
