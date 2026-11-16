package com.fluxisai.task;
import java.time.*; import java.util.UUID;
public record TaskResponse(UUID id,String title,String description,LocalDate dueDate,TaskPriority priority,TaskStatus status,String origin,String relatedEntityType,UUID relatedEntityId,OffsetDateTime createdAt,OffsetDateTime updatedAt){static TaskResponse from(Task t){return new TaskResponse(t.getId(),t.getTitle(),t.getDescription(),t.getDueDate(),t.getPriority(),t.getStatus(),t.getOrigin(),t.getRelatedEntityType(),t.getRelatedEntityId(),t.getCreatedAt(),t.getUpdatedAt());}}
