CREATE TABLE proposals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    valid_until DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    subtotal NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    total NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (total >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_proposals_user_status ON proposals (user_id, status);
CREATE INDEX ix_proposals_user_created_at ON proposals (user_id, created_at DESC);
CREATE TABLE proposal_items (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES proposals (id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services (id) ON DELETE RESTRICT,
    description VARCHAR(500) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 4) NOT NULL CHECK (unit_price >= 0),
    amount NUMERIC(19, 4) NOT NULL CHECK (amount >= 0)
);
CREATE INDEX ix_proposal_items_proposal_id ON proposal_items (proposal_id);
