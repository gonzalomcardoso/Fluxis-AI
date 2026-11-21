CREATE TABLE companies (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    website VARCHAR(2048),
    industry VARCHAR(120),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_companies_user_name_lower
    ON companies (user_id, lower(name));
CREATE INDEX ix_companies_user_created_at
    ON companies (user_id, created_at DESC);

CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_id UUID REFERENCES companies (id) ON DELETE SET NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(320),
    job_title VARCHAR(160),
    origin VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_contacts_user_email_lower
    ON contacts (user_id, lower(email))
    WHERE email IS NOT NULL;
CREATE INDEX ix_contacts_user_created_at
    ON contacts (user_id, created_at DESC);
CREATE INDEX ix_contacts_company_id
    ON contacts (company_id);
