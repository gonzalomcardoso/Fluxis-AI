CREATE TABLE proposal_history (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES proposals (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    change_type VARCHAR(20) NOT NULL CHECK (change_type IN ('CREATED', 'UPDATED')),
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(19, 4) NOT NULL CHECK (subtotal >= 0),
    total NUMERIC(19, 4) NOT NULL CHECK (total >= 0),
    changed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_proposal_history_proposal_changed_at ON proposal_history (proposal_id, changed_at DESC);
