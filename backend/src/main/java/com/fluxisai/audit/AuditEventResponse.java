package com.fluxisai.audit;
import java.time.OffsetDateTime; import java.util.UUID;
public record AuditEventResponse(UUID id,String eventType,String entityType,UUID entityId,String action,String details,OffsetDateTime createdAt){static AuditEventResponse from(AuditEvent e){return new AuditEventResponse(e.getId(),e.getEventType(),e.getEntityType(),e.getEntityId(),e.getAction(),e.getDetails(),e.getCreatedAt());}}
