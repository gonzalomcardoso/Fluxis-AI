package com.fluxisai.task;

import java.util.Map;
import java.util.UUID;

import com.fluxisai.audit.AuditEventService;
import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final AuditEventService audit;
    private final CurrentUser currentUser;

    public TaskService(TaskRepository tasks, AuditEventService audit, CurrentUser currentUser) {
        this.tasks = tasks;
        this.audit = audit;
        this.currentUser = currentUser;
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task(
                currentUser.id(),
                clean(request.title()),
                optional(request.description()),
                request.dueDate(),
                request.priority(),
                request.status(),
                request.origin(),
                request.relatedEntityType(),
                request.relatedEntityId());
        Task saved = tasks.save(task);
        audit.record("TASK", "TASK", saved.getId(), "CREATED", details(saved));
        return TaskResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID id) {
        return TaskResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(Pageable pageable) {
        return PageResponse.from(tasks.findAllByUserId(currentUser.id(), pageable).map(TaskResponse::from));
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = find(id);
        task.update(
                clean(request.title()),
                optional(request.description()),
                request.dueDate(),
                request.priority(),
                request.status(),
                request.origin(),
                request.relatedEntityType(),
                request.relatedEntityId());
        Task saved = tasks.save(task);
        audit.record("TASK", "TASK", id, "UPDATED", details(saved));
        return TaskResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Task task = find(id);
        tasks.delete(task);
        audit.record("TASK", "TASK", id, "DELETED", details(task));
    }

    private Task find(UUID id) {
        return tasks.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la tarea"));
    }

    private static String clean(String value) {
        return value.trim();
    }

    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Map<String, Object> details(Task task) {
        return Map.of("title", task.getTitle(), "status", task.getStatus().name());
    }
}