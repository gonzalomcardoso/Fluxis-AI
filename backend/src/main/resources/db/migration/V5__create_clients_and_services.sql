CREATE TABLE clients (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_id UUID REFERENCES companies (id) ON DELETE SET NULL,
    contact_id UUID REFERENCES contacts (id) ON DELETE SET NULL,
    display_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROSPECT'
        CHECK (status IN ('PROSPECT', 'ACTIVE', 'INACTIVE', 'LOST')),
    origin VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_clients_user_status ON clients (user_id, status);
CREATE INDEX ix_clients_user_created_at ON clients (user_id, created_at DESC);
CREATE INDEX ix_clients_company_id ON clients (company_id);
CREATE INDEX ix_clients_contact_id ON clients (contact_id);

CREATE TABLE services (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(120),
    pricing_model VARCHAR(20) NOT NULL
        CHECK (pricing_model IN ('HOURLY', 'FIXED', 'RECURRING')),
    rate NUMERIC(19, 4) NOT NULL CHECK (rate > 0),
    currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_services_user_name_lower ON services (user_id, lower(name));
CREATE INDEX ix_services_user_status ON services (user_id, status);
CREATE INDEX ix_services_user_available ON services (user_id, available);
