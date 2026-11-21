package com.fluxisai.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String eventType,
        String entityType,
        UUID entityId,
        String action,
        Map<String, Object> details,
        OffsetDateTime createdAt) {

    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getAction(),
                event.getDetails(),
                event.getCreatedAt());
    }
}