package com.fluxisai.task;
import java.time.LocalDate; import java.util.UUID; import jakarta.validation.constraints.*;
public record TaskRequest(@NotBlank @Size(max=200) String title,@Size(max=20000) String description,LocalDate dueDate,TaskPriority priority,TaskStatus status,@Size(max=30) String origin,@Size(max=60) String relatedEntityType,UUID relatedEntityId) {}
