package com.fluxisai.crm;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(UUID id, String name, String website, String industry, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static CompanyResponse from(Company c) { return new CompanyResponse(c.getId(), c.getName(), c.getWebsite(), c.getIndustry(), c.getNotes(), c.getCreatedAt(), c.getUpdatedAt()); }
}
