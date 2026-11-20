-- Option C: visual demonstration dataset for fluxis_test.
-- Run only against the test database. This script never creates or changes passwords.
BEGIN;

CREATE TEMP TABLE option_c_user ON COMMIT DROP AS
SELECT id AS user_id
FROM users
WHERE lower(email) = 'integration@fluxis.local';

DO $$
BEGIN
    IF (SELECT count(*) FROM option_c_user) <> 1 THEN
        RAISE EXCEPTION 'Synthetic Integration User (integration@fluxis.local) must exist before loading Option C';
    END IF;
END $$;

-- Day 7: opportunities used by the application flow.
INSERT INTO job_opportunities (id, user_id, company_name, title, url, source, posted_on, description, requirements, technologies, status, created_at, updated_at)
SELECT x.id, u.user_id, x.company_name, x.title, x.url, 'Option C demo', current_date - x.age_days,
       'Synthetic opportunity for the visual integration dataset.', 'Synthetic requirements.', x.technologies, x.status, now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('71000000-0000-0000-0000-000000000001'::uuid, 'Atlas Labs', 'Java Platform Engineer', 'https://jobs.example.test/option-c/atlas', 5, ARRAY['Java','Spring Boot','PostgreSQL']::text[], 'INTERVIEW'),
    ('71000000-0000-0000-0000-000000000002'::uuid, 'Beacon Systems', 'Frontend Engineer', 'https://jobs.example.test/option-c/beacon', 12, ARRAY['TypeScript','React','Vite']::text[], 'APPLICATION_SENT'),
    ('71000000-0000-0000-0000-000000000003'::uuid, 'Cobalt Cloud', 'Data Engineer', 'https://jobs.example.test/option-c/cobalt', 20, ARRAY['Python','SQL','Docker']::text[], 'OFFER_RECEIVED'),
    ('71000000-0000-0000-0000-000000000004'::uuid, 'Delta Works', 'QA Automation Engineer', 'https://jobs.example.test/option-c/delta', 30, ARRAY['Java','Testing','CI/CD']::text[], 'REJECTED')
) AS x(id, company_name, title, url, age_days, technologies, status)
ON CONFLICT (id) DO NOTHING;

-- Day 8: CRM companies and contacts.
INSERT INTO companies (id, user_id, name, website, industry, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.name, x.website, x.industry, 'Synthetic CRM record for Option C.', now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('81000000-0000-0000-0000-000000000001'::uuid, 'Atlas Labs', 'https://atlas.example.test', 'Technology'),
    ('81000000-0000-0000-0000-000000000002'::uuid, 'Beacon Systems', 'https://beacon.example.test', 'SaaS'),
    ('81000000-0000-0000-0000-000000000003'::uuid, 'Cobalt Cloud', 'https://cobalt.example.test', 'Cloud')
) AS x(id, name, website, industry)
ON CONFLICT (id) DO NOTHING;

INSERT INTO contacts (id, user_id, company_id, first_name, last_name, email, job_title, origin, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.company_id, x.first_name, x.last_name, x.email, x.job_title, 'Option C demo', 'Synthetic contact.', now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('82000000-0000-0000-0000-000000000001'::uuid, '81000000-0000-0000-0000-000000000001'::uuid, 'Ana', 'Torres', 'ana.torres@atlas.example.test', 'Engineering Manager'),
    ('82000000-0000-0000-0000-000000000002'::uuid, '81000000-0000-0000-0000-000000000002'::uuid, 'Bruno', 'Lima', 'bruno.lima@beacon.example.test', 'Product Lead'),
    ('82000000-0000-0000-0000-000000000003'::uuid, '81000000-0000-0000-0000-000000000003'::uuid, 'Carla', 'Meyer', 'carla.meyer@cobalt.example.test', 'CTO'),
    ('82000000-0000-0000-0000-000000000004'::uuid, NULL::uuid, 'Diego', 'Ruiz', 'diego.ruiz@example.test', 'Independent Recruiter')
) AS x(id, company_id, first_name, last_name, email, job_title)
ON CONFLICT (id) DO NOTHING;

-- Day 9: applications, interviews and follow-ups.
INSERT INTO job_applications (id, user_id, job_opportunity_id, status, applied_on, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.opportunity_id, x.status, current_date - x.age_days, 'Synthetic application for Option C.', now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('91000000-0000-0000-0000-000000000001'::uuid, '71000000-0000-0000-0000-000000000001'::uuid, 'INTERVIEW', 4),
    ('91000000-0000-0000-0000-000000000002'::uuid, '71000000-0000-0000-0000-000000000002'::uuid, 'APPLICATION_SENT', 10),
    ('91000000-0000-0000-0000-000000000003'::uuid, '71000000-0000-0000-0000-000000000003'::uuid, 'OFFER_RECEIVED', 18),
    ('91000000-0000-0000-0000-000000000004'::uuid, '71000000-0000-0000-0000-000000000004'::uuid, 'REJECTED', 28)
) AS x(id, opportunity_id, status, age_days)
ON CONFLICT (id) DO NOTHING;

INSERT INTO application_interviews (id, user_id, job_application_id, scheduled_at, interview_type, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.application_id, now() + x.offset_days * interval '1 day', x.interview_type, 'Synthetic interview.', now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('92000000-0000-0000-0000-000000000001'::uuid, '91000000-0000-0000-0000-000000000001'::uuid, 2, 'Technical'),
    ('92000000-0000-0000-0000-000000000002'::uuid, '91000000-0000-0000-0000-000000000001'::uuid, 5, 'Culture fit')
) AS x(id, application_id, offset_days, interview_type)
ON CONFLICT (id) DO NOTHING;

INSERT INTO application_follow_ups (id, user_id, job_application_id, due_on, description, completed, created_at, updated_at)
SELECT x.id, u.user_id, x.application_id, current_date + x.offset_days, x.description, x.completed, now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('93000000-0000-0000-0000-000000000001'::uuid, '91000000-0000-0000-0000-000000000001'::uuid, 3, 'Send interview availability', false),
    ('93000000-0000-0000-0000-000000000002'::uuid, '91000000-0000-0000-0000-000000000002'::uuid, -2, 'Follow up after application', false),
    ('93000000-0000-0000-0000-000000000003'::uuid, '91000000-0000-0000-0000-000000000003'::uuid, -8, 'Archive offer notes', true)
) AS x(id, application_id, offset_days, description, completed)
ON CONFLICT (id) DO NOTHING;

-- Day 10: clients and professional services.
INSERT INTO clients (id, user_id, company_id, contact_id, display_name, status, origin, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.company_id, x.contact_id, x.display_name, x.status, 'Option C demo', 'Synthetic client for dashboard scenarios.', now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('a1000000-0000-0000-0000-000000000001'::uuid, '81000000-0000-0000-0000-000000000001'::uuid, '82000000-0000-0000-0000-000000000001'::uuid, 'Atlas Labs client', 'POTENCIAL'),
    ('a1000000-0000-0000-0000-000000000002'::uuid, '81000000-0000-0000-0000-000000000002'::uuid, '82000000-0000-0000-0000-000000000002'::uuid, 'Beacon Systems client', 'ACTIVE'),
    ('a1000000-0000-0000-0000-000000000003'::uuid, '81000000-0000-0000-0000-000000000003'::uuid, '82000000-0000-0000-0000-000000000003'::uuid, 'Cobalt Cloud client', 'INACTIVE'),
    ('a1000000-0000-0000-0000-000000000004'::uuid, NULL::uuid, '82000000-0000-0000-0000-000000000004'::uuid, 'Delta Works client', 'LOST')
) AS x(id, company_id, contact_id, display_name, status)
ON CONFLICT (id) DO NOTHING;

INSERT INTO services (id, user_id, name, description, category, pricing_model, rate, currency, available, status, notes, created_at, updated_at)
SELECT x.id, u.user_id, x.name, 'Synthetic professional service.', 'Option C', x.pricing_model, x.rate, x.currency, x.available, x.status, 'Demo service.', now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('a2000000-0000-0000-0000-000000000001'::uuid, 'Backend architecture', 'HOURLY', 95.00::numeric, 'EUR', true, 'ACTIVE'),
    ('a2000000-0000-0000-0000-000000000002'::uuid, 'Product discovery workshop', 'FIXED', 1200.00::numeric, 'EUR', true, 'ACTIVE'),
    ('a2000000-0000-0000-0000-000000000003'::uuid, 'Platform maintenance', 'RECURRING', 450.00::numeric, 'USD', true, 'ACTIVE'),
    ('a2000000-0000-0000-0000-000000000004'::uuid, 'Legacy migration', 'FIXED', 800.00::numeric, 'USD', false, 'INACTIVE')
) AS x(id, name, pricing_model, rate, currency, available, status)
ON CONFLICT (id) DO NOTHING;

-- Day 11: proposals, items and history.
INSERT INTO proposals (id, user_id, client_id, title, description, currency, valid_until, status, subtotal, total, created_at, updated_at)
SELECT x.id, u.user_id, x.client_id, x.title, 'Synthetic proposal for Option C.', x.currency, current_date + 30, x.status, x.total, x.total, now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('b1000000-0000-0000-0000-000000000001'::uuid, 'a1000000-0000-0000-0000-000000000002'::uuid, 'Atlas platform proposal', 'EUR', 'ACCEPTED', 2500.00::numeric),
    ('b1000000-0000-0000-0000-000000000002'::uuid, 'a1000000-0000-0000-0000-000000000001'::uuid, 'Discovery proposal', 'EUR', 'SENT', 1200.00::numeric),
    ('b1000000-0000-0000-0000-000000000003'::uuid, 'a1000000-0000-0000-0000-000000000003'::uuid, 'Cloud operations proposal', 'USD', 'ACCEPTED', 1800.00::numeric)
) AS x(id, client_id, title, currency, status, total)
ON CONFLICT (id) DO NOTHING;

INSERT INTO proposal_items (id, proposal_id, service_id, description, quantity, unit_price, amount)
SELECT x.id, x.proposal_id, x.service_id, x.description, x.quantity, x.unit_price, x.quantity * x.unit_price
FROM (VALUES
    ('b2000000-0000-0000-0000-000000000001'::uuid, 'b1000000-0000-0000-0000-000000000001'::uuid, 'a2000000-0000-0000-0000-000000000001'::uuid, 'Architecture hours', 20.0000::numeric, 95.0000::numeric),
    ('b2000000-0000-0000-0000-000000000002'::uuid, 'b1000000-0000-0000-0000-000000000001'::uuid, 'a2000000-0000-0000-0000-000000000002'::uuid, 'Discovery workshop', 1.0000::numeric, 600.0000::numeric),
    ('b2000000-0000-0000-0000-000000000003'::uuid, 'b1000000-0000-0000-0000-000000000002'::uuid, 'a2000000-0000-0000-0000-000000000002'::uuid, 'Discovery workshop', 2.0000::numeric, 600.0000::numeric),
    ('b2000000-0000-0000-0000-000000000004'::uuid, 'b1000000-0000-0000-0000-000000000003'::uuid, 'a2000000-0000-0000-0000-000000000003'::uuid, 'Platform maintenance', 4.0000::numeric, 450.0000::numeric)
) AS x(id, proposal_id, service_id, description, quantity, unit_price)
ON CONFLICT (id) DO NOTHING;

INSERT INTO proposal_history (id, proposal_id, user_id, change_type, status, subtotal, total, changed_at)
SELECT x.id, x.proposal_id, u.user_id, x.change_type, x.status, x.total, x.total, now() - x.age_days * interval '1 day'
FROM option_c_user u
CROSS JOIN (VALUES
    ('b3000000-0000-0000-0000-000000000001'::uuid, 'b1000000-0000-0000-0000-000000000001'::uuid, 'CREATED', 'DRAFT', 2500.00::numeric, 3),
    ('b3000000-0000-0000-0000-000000000002'::uuid, 'b1000000-0000-0000-0000-000000000001'::uuid, 'UPDATED', 'ACCEPTED', 2500.00::numeric, 1),
    ('b3000000-0000-0000-0000-000000000003'::uuid, 'b1000000-0000-0000-0000-000000000002'::uuid, 'CREATED', 'SENT', 1200.00::numeric, 2),
    ('b3000000-0000-0000-0000-000000000004'::uuid, 'b1000000-0000-0000-0000-000000000003'::uuid, 'CREATED', 'ACCEPTED', 1800.00::numeric, 4)
) AS x(id, proposal_id, change_type, status, total, age_days)
ON CONFLICT (id) DO NOTHING;

-- Day 12: recurring costs in different currencies and upcoming renewals.
INSERT INTO subscriptions (id, user_id, name, provider, amount, currency, periodicity, next_charge_date, renewal_date, status, category, notes, management_url, created_at, updated_at)
SELECT x.id, u.user_id, x.name, x.provider, x.amount, x.currency, x.periodicity, current_date + x.next_days, current_date + x.renewal_days, x.status, 'Option C', 'Synthetic subscription.', 'https://example.test/manage', now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('c1000000-0000-0000-0000-000000000001'::uuid, 'Cloud hosting', 'Atlas Cloud', 120.00::numeric, 'EUR', 'MONTHLY', 5, 10, 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000002'::uuid, 'Design toolkit', 'Beacon Tools', 240.00::numeric, 'USD', 'YEARLY', 20, 20, 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000003'::uuid, 'Monitoring', 'Cobalt Monitor', 35.00::numeric, 'EUR', 'MONTHLY', 2, 2, 'ACTIVE'),
    ('c1000000-0000-0000-0000-000000000004'::uuid, 'Old service', 'Delta Vendor', 15.00::numeric, 'EUR', 'MONTHLY', -20, -20, 'CANCELLED')
) AS x(id, name, provider, amount, currency, periodicity, next_days, renewal_days, status)
ON CONFLICT (id) DO NOTHING;

-- Day 13: linked tasks and audit events.
INSERT INTO tasks (id, user_id, title, description, due_date, priority, status, origin, related_entity_type, related_entity_id, created_at, updated_at)
SELECT x.id, u.user_id, x.title, 'Synthetic task for Option C.', current_date + x.due_days, x.priority, x.status, 'OPTION_C', x.entity_type, x.entity_id, now(), now()
FROM option_c_user u
CROSS JOIN (VALUES
    ('d1000000-0000-0000-0000-000000000001'::uuid, 'Prepare accepted proposal handoff', 2, 'HIGH', 'IN_PROGRESS', 'PROPOSAL', 'b1000000-0000-0000-0000-000000000001'::uuid),
    ('d1000000-0000-0000-0000-000000000002'::uuid, 'Review overdue application follow-up', -3, 'URGENT', 'TODO', 'JOB_APPLICATION', '91000000-0000-0000-0000-000000000002'::uuid),
    ('d1000000-0000-0000-0000-000000000003'::uuid, 'Confirm renewal budget', 6, 'MEDIUM', 'TODO', 'SUBSCRIPTION', 'c1000000-0000-0000-0000-000000000001'::uuid),
    ('d1000000-0000-0000-0000-000000000004'::uuid, 'Deliver architecture report', -1, 'HIGH', 'DONE', 'CLIENT', 'a1000000-0000-0000-0000-000000000002'::uuid),
    ('d1000000-0000-0000-0000-000000000005'::uuid, 'Archive cancelled vendor', 0, 'LOW', 'CANCELLED', 'SUBSCRIPTION', 'c1000000-0000-0000-0000-000000000004'::uuid),
    ('d1000000-0000-0000-0000-000000000006'::uuid, 'Prepare dashboard review', 8, 'MEDIUM', 'TODO', 'MANUAL', NULL::uuid)
) AS x(id, title, due_days, priority, status, entity_type, entity_id)
ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_events (id, user_id, event_type, entity_type, entity_id, action, details, created_at)
SELECT x.id, u.user_id, 'TASK', x.entity_type, x.entity_id, x.action, jsonb_build_object('source', 'option-c-demo', 'title', x.title), now() - x.age_days * interval '1 hour'
FROM option_c_user u
CROSS JOIN (VALUES
    ('d2000000-0000-0000-0000-000000000001'::uuid, 'TASK', 'd1000000-0000-0000-0000-000000000001'::uuid, 'CREATED', 'Prepare accepted proposal handoff', 6),
    ('d2000000-0000-0000-0000-000000000002'::uuid, 'TASK', 'd1000000-0000-0000-0000-000000000001'::uuid, 'UPDATED', 'Prepare accepted proposal handoff', 5),
    ('d2000000-0000-0000-0000-000000000003'::uuid, 'TASK', 'd1000000-0000-0000-0000-000000000002'::uuid, 'CREATED', 'Review overdue application follow-up', 4),
    ('d2000000-0000-0000-0000-000000000004'::uuid, 'TASK', 'd1000000-0000-0000-0000-000000000003'::uuid, 'CREATED', 'Confirm renewal budget', 3),
    ('d2000000-0000-0000-0000-000000000005'::uuid, 'TASK', 'd1000000-0000-0000-0000-000000000004'::uuid, 'CREATED', 'Deliver architecture report', 2),
    ('d2000000-0000-0000-0000-000000000006'::uuid, 'TASK', 'd1000000-0000-0000-0000-000000000006'::uuid, 'CREATED', 'Prepare dashboard review', 1)
) AS x(id, entity_type, entity_id, action, title, age_days)
ON CONFLICT (id) DO NOTHING;

COMMIT;
