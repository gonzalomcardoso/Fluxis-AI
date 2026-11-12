CREATE TABLE job_applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    job_opportunity_id UUID NOT NULL REFERENCES job_opportunities (id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'INTERESTING', 'PREPARING_APPLICATION', 'APPLICATION_SENT', 'INTERVIEW', 'OFFER_RECEIVED', 'REJECTED', 'DISCARDED')),
    applied_on DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_job_applications_user_opportunity UNIQUE (user_id, job_opportunity_id)
);

CREATE INDEX ix_job_applications_user_status ON job_applications (user_id, status);
CREATE INDEX ix_job_applications_user_created_at ON job_applications (user_id, created_at DESC);

CREATE TABLE application_interviews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    job_application_id UUID NOT NULL REFERENCES job_applications (id) ON DELETE CASCADE,
    scheduled_at TIMESTAMPTZ NOT NULL,
    interview_type VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_application_interviews_user_scheduled_at ON application_interviews (user_id, scheduled_at);
CREATE INDEX ix_application_interviews_application_id ON application_interviews (job_application_id);

CREATE TABLE application_follow_ups (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    job_application_id UUID NOT NULL REFERENCES job_applications (id) ON DELETE CASCADE,
    due_on DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_application_follow_ups_user_due_on ON application_follow_ups (user_id, due_on);
CREATE INDEX ix_application_follow_ups_application_id ON application_follow_ups (job_application_id);
