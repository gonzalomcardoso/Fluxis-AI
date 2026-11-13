package com.fluxisai.client;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClientResponse(UUID id, UUID companyId, UUID contactId, String displayName, ClientStatus status, String origin, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static ClientResponse from(Client client) { return new ClientResponse(client.getId(), client.getCompanyId(), client.getContactId(), client.getDisplayName(), client.getStatus(), client.getOrigin(), client.getNotes(), client.getCreatedAt(), client.getUpdatedAt()); }
}
