package com.fluxisai.crm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2048) @Pattern(regexp = "^$|https?://\\S+$", message = "La URL debe empezar por http:// o https://") String website,
        @Size(max = 120) String industry,
        @Size(max = 20000) String notes) {}
