package com.fluxisai.crm;

import java.util.UUID;
import jakarta.validation.constraints.*;

public record ContactRequest(
        UUID companyId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 320) String email,
        @Size(max = 160) String jobTitle,
        @Size(max = 100) String origin,
        @Size(max = 20000) String notes) {}
