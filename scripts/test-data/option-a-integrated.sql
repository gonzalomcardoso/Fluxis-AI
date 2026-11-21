-- Option A: complete integrated scenario for fluxis_test.
-- Owner: Synthetic Integration User (integration@fluxis.local).
-- Run only against the test database. This script never creates or changes passwords.
--
-- The dataset is deterministic and idempotent: every row uses a fixed UUID and
-- ON CONFLICT (id) DO NOTHING, so re-running it does not create duplicates.
-- All database content is written in English, per the repository conventions.

BEGIN;

CREATE TEMP TABLE option_a_user ON COMMIT DROP AS
SELECT id AS user_id
FROM users
WHERE lower(email) = 'integration@fluxis.local';

DO $$
BEGIN
    IF (SELECT count(*) FROM option_a_user) <> 1 THEN
        RAISE EXCEPTION 'Synthetic Integration User (integration@fluxis.local) must exist before loading Option A';
    END IF;
END $$;

-- Day 8: CRM companies and contacts -----------------------------------------

INSERT INTO companies (id, user_id, name, website, industry, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.name, x.website, x.industry, 'Option A integrated scenario.', now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa000000-0000-0000-0000-000000000001'::uuid, 'Northwind Analytics', 'https://northwind.example.test', 'Data & Analytics'),
    ('fa000000-0000-0000-0000-000000000002'::uuid, 'Blue Harbor Retail', 'https://blueharbor.example.test', 'Retail Operations'),
    ('fa000000-0000-0000-0000-000000000003'::uuid, 'Vertex Health', 'https://vertexhealth.example.test', 'Healthcare Technology'),
    ('fa000000-0000-0000-0000-000000000004'::uuid, 'Lumen Foundry', 'https://lumenfoundry.example.test', 'Design Studio')
) AS x(id, name, website, industry)
ON CONFLICT (id) DO NOTHING;

INSERT INTO contacts (id, user_id, company_id, first_name, last_name, email, job_title, origin, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.company_id, x.first_name, x.last_name, x.email, x.job_title, 'OPTION_A',
       'Option A integrated scenario: linked contact.', now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa100000-0000-0000-0000-000000000001'::uuid, 'fa000000-0000-0000-0000-000000000001'::uuid, 'Priya', 'Nair', 'priya.nair@northwind.example.test', 'Head of Data'),
    ('fa100000-0000-0000-0000-000000000002'::uuid, 'fa000000-0000-0000-0000-000000000002'::uuid, 'Marcus', 'Bell', 'marcus.bell@blueharbor.example.test', 'Operations Director'),
    ('fa100000-0000-0000-0000-000000000003'::uuid, 'fa000000-0000-0000-0000-000000000003'::uuid, 'Elena', 'Roth', 'elena.roth@vertexhealth.example.test', 'VP Engineering'),
    ('fa100000-0000-0000-0000-000000000004'::uuid, 'fa000000-0000-0000-0000-000000000004'::uuid, 'Tomas', 'Vega', 'tomas.vega@lumenfoundry.example.test', 'Founder'),
    ('fa100000-0000-0000-0000-000000000005'::uuid, NULL::uuid, 'Nadia', 'Okafor', 'nadia.okafor@talentbridge.example.test', 'Independent Recruiter')
) AS x(id, company_id, first_name, last_name, email, job_title)
ON CONFLICT (id) DO NOTHING;

-- Day 7 and day 9: opportunities, applications, interviews and follow-ups ---

INSERT INTO job_opportunities (id, user_id, company_name, title, url, source, posted_on, description, requirements, technologies, status, created_at, updated_at)
SELECT x.id, u.user_id, x.company_name, x.title, x.url, 'Option A integrated scenario',
       current_date - x.age_days,
       'Synthetic opportunity used by the integrated Option A scenario.',
       'Synthetic requirements for the integrated verification.', x.technologies, x.status, now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa200000-0000-0000-0000-000000000001'::uuid, 'Northwind Analytics', 'Senior Platform Engineer', 'https://jobs.example.test/option-a/northwind-platform', 18, ARRAY['Java','Spring Boot','PostgreSQL']::text[], 'INTERVIEW'),
    ('fa200000-0000-0000-0000-000000000002'::uuid, 'Vertex Health', 'Data Platform Engineer', 'https://jobs.example.test/option-a/vertex-data-platform', 25, ARRAY['Python','dbt','Snowflake']::text[], 'OFFER_RECEIVED'),
    ('fa200000-0000-0000-0000-000000000003'::uuid, 'Blue Harbor Retail', 'Engineering Manager', 'https://jobs.example.test/option-a/blue-harbor-manager', 35, ARRAY['Leadership','Retail Systems','Java']::text[], 'REJECTED'),
    ('fa200000-0000-0000-0000-000000000004'::uuid, 'Lumen Foundry', 'Product Engineer', 'https://jobs.example.test/option-a/lumen-product', 45, ARRAY['TypeScript','React','Design Systems']::text[], 'DISCARDED')
) AS x(id, company_name, title, url, age_days, technologies, status)
ON CONFLICT (id) DO NOTHING;

INSERT INTO job_applications (id, user_id, job_opportunity_id, status, applied_on, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.opportunity_id, x.status, current_date - x.age_days,
       'Option A integrated scenario.', now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa300000-0000-0000-0000-000000000001'::uuid, 'fa200000-0000-0000-0000-000000000001'::uuid, 'INTERVIEW', 12),
    ('fa300000-0000-0000-0000-000000000002'::uuid, 'fa200000-0000-0000-0000-000000000002'::uuid, 'OFFER_RECEIVED', 22),
    ('fa300000-0000-0000-0000-000000000003'::uuid, 'fa200000-0000-0000-0000-000000000003'::uuid, 'REJECTED', 32)
) AS x(id, opportunity_id, status, age_days)
ON CONFLICT (id) DO NOTHING;

INSERT INTO application_interviews (id, user_id, job_application_id, scheduled_at, interview_type, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.application_id, now() + x.offset_days * interval '1 day', x.interview_type,
       'Option A integrated scenario.', now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa400000-0000-0000-0000-000000000001'::uuid, 'fa300000-0000-0000-0000-000000000001'::uuid, 2, 'Technical interview'),
    ('fa400000-0000-0000-0000-000000000002'::uuid, 'fa300000-0000-0000-0000-000000000001'::uuid, 5, 'Team interview'),
    ('fa400000-0000-0000-0000-000000000003'::uuid, 'fa300000-0000-0000-0000-000000000002'::uuid, -6, 'Final interview'),
    ('fa400000-0000-0000-0000-000000000004'::uuid, 'fa300000-0000-0000-0000-000000000003'::uuid, -18, 'Screening interview')
) AS x(id, application_id, offset_days, interview_type)
ON CONFLICT (id) DO NOTHING;

INSERT INTO application_follow_ups (id, user_id, job_application_id, due_on, description, completed, created_at, updated_at)
SELECT x.id, u.user_id, x.application_id, current_date + x.offset_days, x.description, x.completed, now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa500000-0000-0000-0000-000000000001'::uuid, 'fa300000-0000-0000-0000-000000000001'::uuid, 3, 'Send updated availability', false),
    ('fa500000-0000-0000-0000-000000000002'::uuid, 'fa300000-0000-0000-0000-000000000002'::uuid, -2, 'Confirm offer decision call', false),
    ('fa500000-0000-0000-0000-000000000003'::uuid, 'fa300000-0000-0000-0000-000000000003'::uuid, -15, 'Archive rejection feedback', true)
) AS x(id, application_id, offset_days, description, completed)
ON CONFLICT (id) DO NOTHING;

-- Day 10: clients and professional services --------------------------------

INSERT INTO clients (id, user_id, company_id, contact_id, display_name, status, origin, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.company_id, x.contact_id, x.display_name, x.status, 'OPTION_A',
       'Option A integrated scenario.', now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa600000-0000-0000-0000-000000000001'::uuid, 'fa000000-0000-0000-0000-000000000001'::uuid, 'fa100000-0000-0000-0000-000000000001'::uuid, 'Northwind Engagement', 'ACTIVE'),
    ('fa600000-0000-0000-0000-000000000002'::uuid, 'fa000000-0000-0000-0000-000000000003'::uuid, 'fa100000-0000-0000-0000-000000000003'::uuid, 'Vertex Health Pilot', 'POTENCIAL'),
    ('fa600000-0000-0000-0000-000000000003'::uuid, 'fa000000-0000-0000-0000-000000000002'::uuid, 'fa100000-0000-0000-0000-000000000002'::uuid, 'Blue Harbor Archive', 'INACTIVE'),
    ('fa600000-0000-0000-0000-000000000004'::uuid, 'fa000000-0000-0000-0000-000000000004'::uuid, 'fa100000-0000-0000-0000-000000000004'::uuid, 'Lumen Foundry Lead', 'LOST')
) AS x(id, company_id, contact_id, display_name, status)
ON CONFLICT (id) DO NOTHING;

INSERT INTO services (id, user_id, name, description, category, pricing_model, rate, currency, available, status, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.name, 'Synthetic professional service for Option A.', 'Option A', x.pricing_model,
       x.rate, x.currency, x.available, x.status, 'Option A integrated scenario.', now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa700000-0000-0000-0000-000000000001'::uuid, 'Data Platform Advisory', 'HOURLY', 95.0000::numeric, 'EUR', true, 'ACTIVE'),
    ('fa700000-0000-0000-0000-000000000002'::uuid, 'Architecture Review', 'FIXED', 1800.0000::numeric, 'EUR', true, 'ACTIVE'),
    ('fa700000-0000-0000-0000-000000000003'::uuid, 'Managed Analytics', 'RECURRING', 600.0000::numeric, 'USD', true, 'ACTIVE'),
    ('fa700000-0000-0000-0000-000000000004'::uuid, 'Data Migration', 'HOURLY', 110.0000::numeric, 'USD', true, 'ACTIVE'),
    ('fa700000-0000-0000-0000-000000000005'::uuid, 'Legacy Workshop', 'FIXED', 900.0000::numeric, 'EUR', false, 'INACTIVE')
) AS x(id, name, pricing_model, rate, currency, available, status)
ON CONFLICT (id) DO NOTHING;

-- Day 11: proposals, items and history -------------------------------------

INSERT INTO proposals (id, user_id, client_id, title, description, currency, valid_until, status, subtotal, total, created_at, updated_at)
SELECT x.id, u.user_id, x.client_id, x.title, 'Option A integrated scenario.', x.currency,
       current_date + x.valid_days, x.status, x.total, x.total, now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa800000-0000-0000-0000-000000000001'::uuid, 'fa600000-0000-0000-0000-000000000001'::uuid, 'Northwind Modern Data Platform', 'EUR', 21, 'ACCEPTED', 3700.0000::numeric),
    ('fa800000-0000-0000-0000-000000000002'::uuid, 'fa600000-0000-0000-0000-000000000002'::uuid, 'Vertex Analytics Accelerator', 'USD', 18, 'ACCEPTED', 3500.0000::numeric),
    ('fa800000-0000-0000-0000-000000000003'::uuid, 'fa600000-0000-0000-0000-000000000003'::uuid, 'Blue Harbor Reporting Refresh', 'EUR', 7, 'REJECTED', 1140.0000::numeric),
    ('fa800000-0000-0000-0000-000000000004'::uuid, 'fa600000-0000-0000-0000-000000000004'::uuid, 'Lumen Discovery Sprint', 'EUR', 14, 'SENT', 570.0000::numeric)
) AS x(id, client_id, title, currency, valid_days, status, total)
ON CONFLICT (id) DO NOTHING;

INSERT INTO proposal_items (id, proposal_id, service_id, description, quantity, unit_price, amount)
SELECT x.id, x.proposal_id, x.service_id, x.description, x.quantity, x.unit_price, x.quantity * x.unit_price
FROM (VALUES
    ('fa810000-0000-0000-0000-000000000001'::uuid, 'fa800000-0000-0000-0000-000000000001'::uuid, 'fa700000-0000-0000-0000-000000000001'::uuid, 'Data platform advisory', 20.0000::numeric, 95.0000::numeric),
    ('fa810000-0000-0000-0000-000000000002'::uuid, 'fa800000-0000-0000-0000-000000000001'::uuid, 'fa700000-0000-0000-0000-000000000002'::uuid, 'Architecture review', 1.0000::numeric, 1800.0000::numeric),
    ('fa810000-0000-0000-0000-000000000003'::uuid, 'fa800000-0000-0000-0000-000000000002'::uuid, 'fa700000-0000-0000-0000-000000000004'::uuid, 'Data migration', 10.0000::numeric, 110.0000::numeric),
    ('fa810000-0000-0000-0000-000000000004'::uuid, 'fa800000-0000-0000-0000-000000000002'::uuid, 'fa700000-0000-0000-0000-000000000003'::uuid, 'Managed analytics', 4.0000::numeric, 600.0000::numeric),
    ('fa810000-0000-0000-0000-000000000005'::uuid, 'fa800000-0000-0000-0000-000000000003'::uuid, 'fa700000-0000-0000-0000-000000000001'::uuid, 'Data platform advisory', 12.0000::numeric, 95.0000::numeric),
    ('fa810000-0000-0000-0000-000000000006'::uuid, 'fa800000-0000-0000-0000-000000000004'::uuid, 'fa700000-0000-0000-0000-000000000001'::uuid, 'Data platform advisory', 6.0000::numeric, 95.0000::numeric)
) AS x(id, proposal_id, service_id, description, quantity, unit_price)
ON CONFLICT (id) DO NOTHING;

INSERT INTO proposal_history (id, proposal_id, user_id, change_type, status, subtotal, total, changed_at)
SELECT x.id, x.proposal_id, u.user_id, x.change_type, x.status, x.total, x.total,
       now() - x.age_days * interval '1 day'
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa820000-0000-0000-0000-000000000001'::uuid, 'fa800000-0000-0000-0000-000000000001'::uuid, 'CREATED', 'DRAFT', 0.0000::numeric, 20),
    ('fa820000-0000-0000-0000-000000000002'::uuid, 'fa800000-0000-0000-0000-000000000001'::uuid, 'UPDATED', 'SENT', 3700.0000::numeric, 18),
    ('fa820000-0000-0000-0000-000000000003'::uuid, 'fa800000-0000-0000-0000-000000000001'::uuid, 'UPDATED', 'ACCEPTED', 3700.0000::numeric, 14),
    ('fa820000-0000-0000-0000-000000000004'::uuid, 'fa800000-0000-0000-0000-000000000002'::uuid, 'CREATED', 'DRAFT', 0.0000::numeric, 15),
    ('fa820000-0000-0000-0000-000000000005'::uuid, 'fa800000-0000-0000-0000-000000000002'::uuid, 'UPDATED', 'SENT', 3500.0000::numeric, 13),
    ('fa820000-0000-0000-0000-000000000006'::uuid, 'fa800000-0000-0000-0000-000000000002'::uuid, 'UPDATED', 'ACCEPTED', 3500.0000::numeric, 9),
    ('fa820000-0000-0000-0000-000000000007'::uuid, 'fa800000-0000-0000-0000-000000000003'::uuid, 'CREATED', 'SENT', 1140.0000::numeric, 8),
    ('fa820000-0000-0000-0000-000000000008'::uuid, 'fa800000-0000-0000-0000-000000000003'::uuid, 'UPDATED', 'REJECTED', 1140.0000::numeric, 4),
    ('fa820000-0000-0000-0000-000000000009'::uuid, 'fa800000-0000-0000-0000-000000000004'::uuid, 'CREATED', 'SENT', 570.0000::numeric, 1)
) AS x(id, proposal_id, change_type, status, total, age_days)
ON CONFLICT (id) DO NOTHING;

-- Day 12: subscriptions in EUR and USD -------------------------------------

INSERT INTO subscriptions (id, user_id, name, provider, amount, currency, periodicity, next_charge_date, renewal_date, status, category, notes, management_url, created_at, updated_at)
SELECT x.id, u.user_id, x.name, x.provider, x.amount, x.currency, x.periodicity,
       current_date + x.next_days, current_date + x.renewal_days, x.status, 'OPTION_A',
       'Option A integrated scenario.', 'https://billing.example.test/option-a', now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('fa900000-0000-0000-0000-000000000001'::uuid, 'Cloud hosting', 'Cirrus Hosting', 120.00::numeric, 'EUR', 'MONTHLY', 5, 10, 'ACTIVE'),
    ('fa900000-0000-0000-0000-000000000002'::uuid, 'Design suite', 'PixelWorks', 480.00::numeric, 'EUR', 'YEARLY', 40, 40, 'ACTIVE'),
    ('fa900000-0000-0000-0000-000000000003'::uuid, 'Observability', 'Beacon Monitor', 300.00::numeric, 'USD', 'MONTHLY', 15, 15, 'ACTIVE'),
    ('fa900000-0000-0000-0000-000000000004'::uuid, 'Email platform', 'Postmark Pro', 180.00::numeric, 'USD', 'QUARTERLY', 25, 25, 'ACTIVE'),
    ('fa900000-0000-0000-0000-000000000005'::uuid, 'Data warehouse', 'Snowline', 3200.00::numeric, 'USD', 'MONTHLY', 20, 20, 'ACTIVE'),
    ('fa900000-0000-0000-0000-000000000006'::uuid, 'Legacy fax', 'FaxBridge', 25.00::numeric, 'USD', 'MONTHLY', -30, -30, 'CANCELLED')
) AS x(id, name, provider, amount, currency, periodicity, next_days, renewal_days, status)
ON CONFLICT (id) DO NOTHING;

-- Day 13: linked tasks and audit events ------------------------------------

INSERT INTO tasks (id, user_id, title, description, due_date, priority, status, origin, related_entity_type, related_entity_id, created_at, updated_at)
SELECT x.id, u.user_id, x.title, 'Option A integrated scenario.', current_date + x.due_days,
       x.priority, x.status, 'OPTION_A', x.entity_type, x.entity_id, now(), now()
FROM option_a_user u
CROSS JOIN (VALUES
    ('faa00000-0000-0000-0000-000000000001'::uuid, 'Prepare Northwind kickoff', 3, 'HIGH', 'TODO', 'CLIENT', 'fa600000-0000-0000-0000-000000000001'::uuid),
    ('faa00000-0000-0000-0000-000000000002'::uuid, 'Send Vertex proposal follow-up', -2, 'URGENT', 'TODO', 'PROPOSAL', 'fa800000-0000-0000-0000-000000000002'::uuid),
    ('faa00000-0000-0000-0000-000000000003'::uuid, 'Review warehouse renewal', 5, 'HIGH', 'IN_PROGRESS', 'SUBSCRIPTION', 'fa900000-0000-0000-0000-000000000005'::uuid),
    ('faa00000-0000-0000-0000-000000000004'::uuid, 'Close Blue Harbor proposal', -12, 'MEDIUM', 'DONE', 'PROPOSAL', 'fa800000-0000-0000-0000-000000000003'::uuid),
    ('faa00000-0000-0000-0000-000000000005'::uuid, 'Update Lumen contact', -5, 'LOW', 'TODO', 'CLIENT', 'fa600000-0000-0000-0000-000000000004'::uuid),
    ('faa00000-0000-0000-0000-000000000006'::uuid, 'Archive rejected application', -10, 'LOW', 'DONE', 'JOB_APPLICATION', 'fa300000-0000-0000-0000-000000000003'::uuid),
    ('faa00000-0000-0000-0000-000000000007'::uuid, 'Cancel duplicate renewal reminder', -1, 'LOW', 'CANCELLED', 'SUBSCRIPTION', 'fa900000-0000-0000-0000-000000000002'::uuid),
    ('faa00000-0000-0000-0000-000000000008'::uuid, 'Plan weekly focus', NULL::integer, 'MEDIUM', 'TODO', NULL, NULL::uuid)
) AS x(id, title, due_days, priority, status, entity_type, entity_id)
ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_events (id, user_id, event_type, entity_type, entity_id, action, details, created_at)
SELECT x.id, u.user_id, x.entity_type, x.entity_type, x.entity_id, x.action,
       jsonb_build_object('source', 'option-a-integrated', 'title', x.title),
       now() - x.age_days * interval '1 day'
FROM option_a_user u
CROSS JOIN (VALUES
    ('fab00000-0000-0000-0000-000000000001'::uuid, 'TASK', 'faa00000-0000-0000-0000-000000000001'::uuid, 'CREATED', 'Prepare Northwind kickoff', 6),
    ('fab00000-0000-0000-0000-000000000002'::uuid, 'TASK', 'faa00000-0000-0000-0000-000000000001'::uuid, 'UPDATED', 'Prepare Northwind kickoff', 2),
    ('fab00000-0000-0000-0000-000000000003'::uuid, 'TASK', 'faa00000-0000-0000-0000-000000000002'::uuid, 'CREATED', 'Send Vertex proposal follow-up', 5),
    ('fab00000-0000-0000-0000-000000000004'::uuid, 'TASK', 'faa00000-0000-0000-0000-000000000003'::uuid, 'CREATED', 'Review warehouse renewal', 4),
    ('fab00000-0000-0000-0000-000000000005'::uuid, 'TASK', 'faa00000-0000-0000-0000-000000000004'::uuid, 'CREATED', 'Close Blue Harbor proposal', 12),
    ('fab00000-0000-0000-0000-000000000006'::uuid, 'TASK', 'faa00000-0000-0000-0000-000000000004'::uuid, 'UPDATED', 'Close Blue Harbor proposal', 11),
    ('fab00000-0000-0000-0000-000000000007'::uuid, 'PROPOSAL', 'fa800000-0000-0000-0000-000000000001'::uuid, 'CREATED', 'Northwind Modern Data Platform', 20),
    ('fab00000-0000-0000-0000-000000000008'::uuid, 'PROPOSAL', 'fa800000-0000-0000-0000-000000000001'::uuid, 'UPDATED', 'Northwind Modern Data Platform', 14),
    ('fab00000-0000-0000-0000-000000000009'::uuid, 'CLIENT', 'fa600000-0000-0000-0000-000000000001'::uuid, 'UPDATED', 'Northwind Engagement', 9),
    ('fab00000-0000-0000-0000-000000000010'::uuid, 'SUBSCRIPTION', 'fa900000-0000-0000-0000-000000000005'::uuid, 'CREATED', 'Data warehouse', 7)
) AS x(id, entity_type, entity_id, action, title, age_days)
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Quick summary of the records owned by this scenario.
WITH u AS (
    SELECT id AS user_id FROM users WHERE lower(email) = 'integration@fluxis.local'
)
SELECT 'companies' AS entity, count(*) AS rows FROM companies c, u WHERE c.user_id = u.user_id AND c.notes = 'Option A integrated scenario.'
UNION ALL SELECT 'contacts', count(*) FROM contacts c, u WHERE c.user_id = u.user_id AND c.origin = 'OPTION_A'
UNION ALL SELECT 'job_opportunities', count(*) FROM job_opportunities o, u WHERE o.user_id = u.user_id AND o.source = 'Option A integrated scenario'
UNION ALL SELECT 'job_applications', count(*) FROM job_applications a, u WHERE a.user_id = u.user_id AND a.notes = 'Option A integrated scenario.'
UNION ALL SELECT 'clients', count(*) FROM clients c, u WHERE c.user_id = u.user_id AND c.origin = 'OPTION_A'
UNION ALL SELECT 'services', count(*) FROM services s, u WHERE s.user_id = u.user_id AND s.notes = 'Option A integrated scenario.'
UNION ALL SELECT 'proposals', count(*) FROM proposals p, u WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.'
UNION ALL SELECT 'subscriptions', count(*) FROM subscriptions s, u WHERE s.user_id = u.user_id AND s.category = 'OPTION_A'
UNION ALL SELECT 'tasks', count(*) FROM tasks t, u WHERE t.user_id = u.user_id AND t.origin = 'OPTION_A'
UNION ALL SELECT 'audit_events', count(*) FROM audit_events a, u WHERE a.user_id = u.user_id AND a.details->>'source' = 'option-a-integrated'
ORDER BY entity;
