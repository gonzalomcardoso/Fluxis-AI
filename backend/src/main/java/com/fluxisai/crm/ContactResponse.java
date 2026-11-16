package com.fluxisai.crm;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContactResponse(UUID id, UUID companyId, String firstName, String lastName, String email, String jobTitle, String origin, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static ContactResponse from(Contact c) { return new ContactResponse(c.getId(), c.getCompanyId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getJobTitle(), c.getOrigin(), c.getNotes(), c.getCreatedAt(), c.getUpdatedAt()); }
}
