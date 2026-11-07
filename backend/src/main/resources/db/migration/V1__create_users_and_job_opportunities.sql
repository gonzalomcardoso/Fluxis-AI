CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE job_opportunities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_name VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(2048),
    source VARCHAR(100),
    posted_on DATE,
    description TEXT,
    requirements TEXT,
    technologies TEXT[] NOT NULL DEFAULT '{}',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING',
            'INTERESTING',
            'PREPARING_APPLICATION',
            'APPLICATION_SENT',
            'INTERVIEW',
            'OFFER_RECEIVED',
            'REJECTED',
            'DISCARDED'
        )),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_job_opportunities_user_created_at
    ON job_opportunities (user_id, created_at DESC);

CREATE INDEX ix_job_opportunities_user_status
    ON job_opportunities (user_id, status);

CREATE UNIQUE INDEX ux_job_opportunities_user_url
    ON job_opportunities (user_id, url)
    WHERE url IS NOT NULL;
