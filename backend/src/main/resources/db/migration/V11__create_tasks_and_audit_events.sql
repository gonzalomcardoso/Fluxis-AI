CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_date DATE,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    origin VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    related_entity_type VARCHAR(60),
    related_entity_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_tasks_user_status_due_date ON tasks (user_id, status, due_date);
CREATE INDEX ix_tasks_related_entity ON tasks (user_id, related_entity_type, related_entity_id);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID,
    action VARCHAR(30) NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_audit_events_user_created_at ON audit_events (user_id, created_at DESC);
CREATE INDEX ix_audit_events_entity ON audit_events (user_id, entity_type, entity_id);
