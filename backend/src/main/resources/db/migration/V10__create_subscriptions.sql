CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    provider VARCHAR(200) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    periodicity VARCHAR(20) NOT NULL CHECK (periodicity IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    next_charge_date DATE,
    renewal_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED', 'EXPIRED')),
    category VARCHAR(120),
    notes TEXT,
    management_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_subscriptions_user_currency ON subscriptions (user_id, currency);
CREATE INDEX ix_subscriptions_user_renewal_date ON subscriptions (user_id, renewal_date);
