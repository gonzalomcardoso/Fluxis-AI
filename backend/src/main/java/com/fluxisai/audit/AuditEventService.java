package com.fluxisai.audit;

import java.util.Map;
import java.util.UUID;

import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {
    private final AuditEventRepository events;
    private final CurrentUser currentUser;

    public AuditEventService(AuditEventRepository events, CurrentUser currentUser) {
        this.events = events;
        this.currentUser = currentUser;
    }

    @Transactional
    public void record(
            String eventType,
            String entityType,
            UUID entityId,
            String action,
            Map<String, Object> details) {
        events.save(new AuditEvent(currentUser.id(), eventType, entityType, entityId, action, details));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> list(Pageable pageable) {
        return PageResponse.from(events.findAllByUserId(currentUser.id(), pageable).map(AuditEventResponse::from));
    }
}