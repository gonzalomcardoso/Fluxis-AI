package com.fluxisai.client;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        UUID companyId,
        UUID contactId,
        @NotBlank @Size(max = 200) String displayName,
        ClientStatus status,
        @Size(max = 100) String origin,
        @Size(max = 20000) String notes) {}
