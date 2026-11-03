package com.fluxisai.job;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateJobOpportunityRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2048)
        @Pattern(regexp = "^https?://\\S+$", message = "La URL debe empezar por http:// o https://")
        String url,
        @Size(max = 100) String source,
        LocalDate postedOn,
        @Size(max = 20000) String description,
        @Size(max = 20000) String requirements,
        @Size(max = 30) List<@NotBlank @Size(max = 50) String> technologies,
        JobStatus status) {
}
