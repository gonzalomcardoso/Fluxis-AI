-- Option A verification: read-only checks over the integrated scenario.
-- Scope: Synthetic Integration User (integration@fluxis.local) in fluxis_test.
--
-- Every automated case is evaluated with the same rules used by the backend
-- dashboard. The two UI cases at the end stay Pending until a person confirms
-- them in the browser.

BEGIN;

CREATE TEMP TABLE option_a_user ON COMMIT DROP AS
SELECT id AS user_id
FROM users
WHERE lower(email) = 'integration@fluxis.local';

CREATE TEMP TABLE option_a_metrics AS
WITH base AS (
    SELECT
        u.user_id,
        (SELECT count(*) FROM users WHERE lower(email) = 'integration@fluxis.local') AS user_count,
        (SELECT count(*) FROM flyway_schema_history
          WHERE success AND version ~ '^[0-9]+$' AND version::int BETWEEN 1 AND 11) AS migrations_applied,
        (SELECT count(*) FROM flyway_schema_history WHERE NOT success) AS migrations_failed,
        (SELECT count(*) FROM (
            SELECT user_id FROM companies WHERE notes = 'Option A integrated scenario.'
            UNION ALL SELECT user_id FROM contacts WHERE origin = 'OPTION_A'
            UNION ALL SELECT user_id FROM job_opportunities WHERE source = 'Option A integrated scenario'
            UNION ALL SELECT user_id FROM job_applications WHERE notes = 'Option A integrated scenario.'
            UNION ALL SELECT user_id FROM application_interviews WHERE notes = 'Option A integrated scenario.'
            UNION ALL SELECT user_id FROM clients WHERE origin = 'OPTION_A'
            UNION ALL SELECT user_id FROM services WHERE notes = 'Option A integrated scenario.'
            UNION ALL SELECT user_id FROM proposals WHERE description = 'Option A integrated scenario.'
            UNION ALL SELECT user_id FROM subscriptions WHERE category = 'OPTION_A'
            UNION ALL SELECT user_id FROM tasks WHERE origin = 'OPTION_A'
            UNION ALL SELECT user_id FROM audit_events WHERE details->>'source' = 'option-a-integrated'
        ) scoped WHERE scoped.user_id <> u.user_id) AS cross_user_rows,

        (SELECT count(*) FROM companies WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.') AS companies,
        (SELECT count(*) FROM contacts WHERE user_id = u.user_id AND origin = 'OPTION_A') AS contacts,
        (SELECT count(*) FROM contacts WHERE user_id = u.user_id AND origin = 'OPTION_A' AND company_id IS NOT NULL) AS contacts_with_company,
        (SELECT count(*) FROM contacts WHERE user_id = u.user_id AND origin = 'OPTION_A' AND company_id IS NULL) AS contacts_without_company,
        (SELECT count(*) FROM (
            SELECT lower(name) FROM companies
            WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.'
            GROUP BY lower(name) HAVING count(*) > 1
        ) d) AS duplicate_companies,
        (SELECT count(*) FROM (
            SELECT lower(email) FROM contacts
            WHERE user_id = u.user_id AND origin = 'OPTION_A' AND email IS NOT NULL
            GROUP BY lower(email) HAVING count(*) > 1
        ) d) AS duplicate_contact_emails,

        (SELECT count(*) FROM job_opportunities WHERE user_id = u.user_id AND source = 'Option A integrated scenario') AS opportunities,
        (SELECT count(DISTINCT status) FROM job_opportunities WHERE user_id = u.user_id AND source = 'Option A integrated scenario') AS opportunity_statuses,
        (SELECT count(*) FROM job_applications WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.') AS applications,
        (SELECT count(DISTINCT job_opportunity_id) FROM job_applications WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.') AS applications_distinct_opportunities,
        (SELECT count(*) FROM job_applications WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND status = 'INTERVIEW') AS app_interview,
        (SELECT count(*) FROM job_applications WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND status = 'OFFER_RECEIVED') AS app_offer,
        (SELECT count(*) FROM job_applications WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND status = 'REJECTED') AS app_rejected,
        (SELECT count(*) FROM application_interviews WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.') AS interviews,
        (SELECT count(*) FROM application_interviews WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND scheduled_at > now()) AS interviews_future,
        (SELECT count(*) FROM application_follow_ups f
          JOIN job_applications a ON a.id = f.job_application_id
         WHERE f.user_id = u.user_id AND a.notes = 'Option A integrated scenario.') AS followups,
        (SELECT count(*) FROM application_follow_ups f
          JOIN job_applications a ON a.id = f.job_application_id
         WHERE f.user_id = u.user_id AND a.notes = 'Option A integrated scenario.' AND NOT f.completed) AS followups_open,
        (SELECT count(*) FROM application_follow_ups f
          JOIN job_applications a ON a.id = f.job_application_id
         WHERE f.user_id = u.user_id AND a.notes = 'Option A integrated scenario.'
           AND NOT f.completed AND f.due_on < current_date) AS followups_overdue_open,
        (SELECT count(*) FROM application_follow_ups f
          JOIN job_applications a ON a.id = f.job_application_id
         WHERE f.user_id = u.user_id AND a.notes = 'Option A integrated scenario.' AND f.completed) AS followups_completed,

        (SELECT count(*) FROM clients WHERE user_id = u.user_id AND origin = 'OPTION_A') AS clients,
        (SELECT count(*) FROM clients WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'POTENCIAL') AS client_potencial,
        (SELECT count(*) FROM clients WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'ACTIVE') AS client_active,
        (SELECT count(*) FROM clients WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'INACTIVE') AS client_inactive,
        (SELECT count(*) FROM clients WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'LOST') AS client_lost,
        (SELECT count(*) FROM clients WHERE user_id = u.user_id AND origin = 'OPTION_A' AND company_id IS NOT NULL AND contact_id IS NOT NULL) AS clients_linked,

        (SELECT count(*) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.') AS services,
        (SELECT count(*) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND status = 'ACTIVE') AS services_active,
        (SELECT count(*) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND status = 'INACTIVE') AS services_inactive,
        (SELECT count(*) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND pricing_model = 'HOURLY') AS services_hourly,
        (SELECT count(*) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND pricing_model = 'FIXED') AS services_fixed,
        (SELECT count(*) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND pricing_model = 'RECURRING') AS services_recurring,
        (SELECT count(*) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.' AND rate > 0) AS services_positive_rate,
        (SELECT count(DISTINCT currency) FROM services WHERE user_id = u.user_id AND notes = 'Option A integrated scenario.') AS service_currencies,

        (SELECT count(*) FROM proposals WHERE user_id = u.user_id AND description = 'Option A integrated scenario.') AS proposals,
        (SELECT count(*) FROM proposal_items i JOIN proposals p ON p.id = i.proposal_id
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.') AS items,
        (SELECT count(*) FROM proposals p
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.'
            AND p.subtotal <> COALESCE((SELECT sum(i.amount) FROM proposal_items i WHERE i.proposal_id = p.id), 0)) AS proposal_subtotal_mismatches,
        (SELECT count(*) FROM proposals p
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.'
            AND p.total <> p.subtotal) AS proposal_total_mismatches,
        (SELECT count(*) FROM proposal_items i JOIN proposals p ON p.id = i.proposal_id
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.'
            AND i.amount <> i.quantity * i.unit_price) AS item_amount_mismatches,
        (SELECT count(*) FROM proposals WHERE user_id = u.user_id AND description = 'Option A integrated scenario.' AND status = 'ACCEPTED') AS proposal_accepted,
        (SELECT count(*) FROM proposals WHERE user_id = u.user_id AND description = 'Option A integrated scenario.' AND status = 'REJECTED') AS proposal_rejected,
        (SELECT count(*) FROM proposals WHERE user_id = u.user_id AND description = 'Option A integrated scenario.' AND status = 'SENT') AS proposal_sent,
        (SELECT count(DISTINCT currency) FROM proposals
          WHERE user_id = u.user_id AND description = 'Option A integrated scenario.' AND status = 'ACCEPTED') AS accepted_currencies,
        (SELECT count(*) FROM (
            SELECT currency FROM proposals
            WHERE user_id = u.user_id AND description = 'Option A integrated scenario.' AND status = 'ACCEPTED'
            UNION
            SELECT currency FROM subscriptions
            WHERE user_id = u.user_id AND category = 'OPTION_A' AND status = 'ACTIVE'
        ) currencies) AS margin_currencies,
        (SELECT COALESCE(sum(total), 0)::numeric(19,2) FROM proposals
          WHERE user_id = u.user_id AND description = 'Option A integrated scenario.'
            AND status = 'ACCEPTED' AND currency = 'EUR') AS accepted_eur,
        (SELECT COALESCE(sum(total), 0)::numeric(19,2) FROM proposals
          WHERE user_id = u.user_id AND description = 'Option A integrated scenario.'
            AND status = 'ACCEPTED' AND currency = 'USD') AS accepted_usd,
        (SELECT count(*) FROM proposal_history h JOIN proposals p ON p.id = h.proposal_id
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.') AS history,
        (SELECT count(DISTINCT h.proposal_id) FROM proposal_history h JOIN proposals p ON p.id = h.proposal_id
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.') AS proposals_with_history,
        (SELECT count(*) FROM proposal_history h JOIN proposals p ON p.id = h.proposal_id
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.'
            AND h.change_type = 'CREATED') AS history_created,
        (SELECT count(*) FROM proposal_history h JOIN proposals p ON p.id = h.proposal_id
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.'
            AND h.change_type = 'UPDATED') AS history_updated,
        (SELECT count(*) FROM proposals p
          WHERE p.user_id = u.user_id AND p.description = 'Option A integrated scenario.'
            AND (SELECT h.total FROM proposal_history h WHERE h.proposal_id = p.id
                  ORDER BY h.changed_at DESC LIMIT 1) IS DISTINCT FROM p.total) AS history_latest_mismatches,

        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A') AS subscriptions,
        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A' AND status = 'ACTIVE') AS subs_active,
        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A' AND status = 'CANCELLED') AS subs_cancelled,
        (SELECT count(DISTINCT currency) FROM subscriptions
          WHERE user_id = u.user_id AND category = 'OPTION_A' AND status = 'ACTIVE') AS subs_currencies,
        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
          AND status = 'ACTIVE' AND periodicity = 'MONTHLY') AS subs_monthly,
        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
          AND status = 'ACTIVE' AND periodicity = 'QUARTERLY') AS subs_quarterly,
        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
          AND status = 'ACTIVE' AND periodicity = 'YEARLY') AS subs_yearly,
        (SELECT COALESCE(sum(CASE periodicity
                    WHEN 'WEEKLY' THEN round(amount * 52 / 12, 2)
                    WHEN 'MONTHLY' THEN amount
                    WHEN 'QUARTERLY' THEN round(amount / 3, 2)
                    WHEN 'YEARLY' THEN round(amount / 12, 2) END), 0)::numeric(19,2)
          FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
            AND status = 'ACTIVE' AND currency = 'EUR') AS monthly_eur,
        (SELECT COALESCE(sum(CASE periodicity
                    WHEN 'WEEKLY' THEN round(amount * 52 / 12, 2)
                    WHEN 'MONTHLY' THEN amount
                    WHEN 'QUARTERLY' THEN round(amount / 3, 2)
                    WHEN 'YEARLY' THEN round(amount / 12, 2) END), 0)::numeric(19,2)
          FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
            AND status = 'ACTIVE' AND currency = 'USD') AS monthly_usd,
        (SELECT COALESCE(sum(CASE periodicity
                    WHEN 'WEEKLY' THEN amount * 52
                    WHEN 'MONTHLY' THEN amount * 12
                    WHEN 'QUARTERLY' THEN amount * 4
                    WHEN 'YEARLY' THEN amount END), 0)::numeric(19,2)
          FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
            AND status = 'ACTIVE' AND currency = 'EUR') AS annual_eur,
        (SELECT COALESCE(sum(CASE periodicity
                    WHEN 'WEEKLY' THEN amount * 52
                    WHEN 'MONTHLY' THEN amount * 12
                    WHEN 'QUARTERLY' THEN amount * 4
                    WHEN 'YEARLY' THEN amount END), 0)::numeric(19,2)
          FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
            AND status = 'ACTIVE' AND currency = 'USD') AS annual_usd,
        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
          AND status = 'ACTIVE' AND renewal_date IS NOT NULL
          AND renewal_date <= current_date + 30) AS subs_upcoming,
        (SELECT count(*) FROM subscriptions WHERE user_id = u.user_id AND category = 'OPTION_A'
          AND status = 'ACTIVE' AND renewal_date IS NOT NULL) AS subs_active_with_renewal,

        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A') AS tasks,
        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'TODO') AS task_todo,
        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'IN_PROGRESS') AS task_in_progress,
        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'DONE') AS task_done,
        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A' AND status = 'CANCELLED') AS task_cancelled,
        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A'
          AND due_date IS NOT NULL AND due_date < current_date
          AND status NOT IN ('DONE', 'CANCELLED')) AS task_overdue,
        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A'
          AND related_entity_type IS NOT NULL AND related_entity_id IS NOT NULL) AS task_linked,
        (SELECT count(*) FROM tasks WHERE user_id = u.user_id AND origin = 'OPTION_A'
          AND related_entity_type IS NULL AND related_entity_id IS NULL) AS task_manual,

        (SELECT count(*) FROM audit_events WHERE user_id = u.user_id AND details->>'source' = 'option-a-integrated') AS audit_events,
        (SELECT count(DISTINCT entity_type) FROM audit_events WHERE user_id = u.user_id AND details->>'source' = 'option-a-integrated') AS audit_entity_types,
        (SELECT count(DISTINCT action) FROM audit_events WHERE user_id = u.user_id AND details->>'source' = 'option-a-integrated') AS audit_actions,
        (SELECT count(*) FROM audit_events a
          WHERE a.user_id = u.user_id AND a.details->>'source' = 'option-a-integrated'
            AND NOT (
                (a.entity_type = 'TASK' AND EXISTS (SELECT 1 FROM tasks t WHERE t.id = a.entity_id))
                OR (a.entity_type = 'PROPOSAL' AND EXISTS (SELECT 1 FROM proposals p WHERE p.id = a.entity_id))
                OR (a.entity_type = 'CLIENT' AND EXISTS (SELECT 1 FROM clients c WHERE c.id = a.entity_id))
                OR (a.entity_type = 'SUBSCRIPTION' AND EXISTS (SELECT 1 FROM subscriptions s WHERE s.id = a.entity_id))
            )) AS audit_orphans,
        (SELECT count(*) FROM audit_events WHERE user_id = u.user_id AND details->>'source' = 'option-a-integrated' AND details IS NOT NULL) AS audit_with_details
    FROM option_a_user u
)
SELECT b.*,
       (accepted_eur - monthly_eur)::numeric(19,2) AS margin_eur,
       (accepted_usd - monthly_usd)::numeric(19,2) AS margin_usd
FROM base b;

CREATE TEMP TABLE option_a_checks (
    day integer NOT NULL,
    module text NOT NULL,
    case_name text NOT NULL,
    expected text NOT NULL,
    actual text NOT NULL,
    passed boolean
);

INSERT INTO option_a_checks (day, module, case_name, expected, actual, passed)
SELECT v.day, v.module, v.case_name, v.expected, v.actual, v.passed
FROM option_a_metrics m
CROSS JOIN LATERAL (VALUES
    -- Day 7: foundations and user isolation
    (7, 'Fundamentos', 'Synthetic Integration User exists exactly once', '1', m.user_count::text, m.user_count = 1),
    (7, 'Fundamentos', 'Flyway migrations V1-V11 applied successfully', '11', m.migrations_applied::text, m.migrations_applied = 11 AND m.migrations_failed = 0),
    (7, 'Fundamentos', 'No Option A row is assigned to another user', '0', m.cross_user_rows::text, m.cross_user_rows = 0),
    (7, 'Fundamentos', 'Base opportunities visible', '4', m.opportunities::text, m.opportunities = 4),
    (7, 'Fundamentos', 'Base companies visible', '4', m.companies::text, m.companies = 4),
    (7, 'Fundamentos', 'Base contacts visible', '5', m.contacts::text, m.contacts = 5),
    (7, 'Fundamentos', 'Base applications visible', '3', m.applications::text, m.applications = 3),

    -- Day 8: CRM
    (8, 'CRM', 'Companies created', '4', m.companies::text, m.companies = 4),
    (8, 'CRM', 'Contacts created', '5', m.contacts::text, m.contacts = 5),
    (8, 'CRM', 'Contacts linked to a company', '4', m.contacts_with_company::text, m.contacts_with_company = 4),
    (8, 'CRM', 'Independent contact without company', '1', m.contacts_without_company::text, m.contacts_without_company = 1),
    (8, 'CRM', 'No duplicate company names', '0', m.duplicate_companies::text, m.duplicate_companies = 0),
    (8, 'CRM', 'No duplicate contact emails', '0', m.duplicate_contact_emails::text, m.duplicate_contact_emails = 0),

    -- Day 9: applications
    (9, 'Candidaturas', 'Opportunities in the application flow', '4', m.opportunities::text, m.opportunities = 4),
    (9, 'Candidaturas', 'Distinct opportunity statuses', '4', m.opportunity_statuses::text, m.opportunity_statuses = 4),
    (9, 'Candidaturas', 'Applications created', '3', m.applications::text, m.applications = 3),
    (9, 'Candidaturas', 'One application per opportunity', '3', m.applications_distinct_opportunities::text, m.applications_distinct_opportunities = 3),
    (9, 'Candidaturas', 'Application status distribution', 'INTERVIEW=1, OFFER_RECEIVED=1, REJECTED=1',
        'INTERVIEW=' || m.app_interview || ', OFFER_RECEIVED=' || m.app_offer || ', REJECTED=' || m.app_rejected,
        m.app_interview = 1 AND m.app_offer = 1 AND m.app_rejected = 1),
    (9, 'Candidaturas', 'Interviews registered', '4', m.interviews::text, m.interviews = 4),
    (9, 'Candidaturas', 'Future interviews', '2', m.interviews_future::text, m.interviews_future = 2),
    (9, 'Candidaturas', 'Follow-ups registered', '3', m.followups::text, m.followups = 3),
    (9, 'Candidaturas', 'Open overdue follow-up', '1', m.followups_overdue_open::text, m.followups_overdue_open = 1),
    (9, 'Candidaturas', 'Completed follow-up', '1', m.followups_completed::text, m.followups_completed = 1),

    -- Day 10: clients and services
    (10, 'Clientes y servicios', 'Clients created', '4', m.clients::text, m.clients = 4),
    (10, 'Clientes y servicios', 'Client status distribution', 'POTENCIAL=1, ACTIVE=1, INACTIVE=1, LOST=1',
        'POTENCIAL=' || m.client_potencial || ', ACTIVE=' || m.client_active || ', INACTIVE=' || m.client_inactive || ', LOST=' || m.client_lost,
        m.client_potencial = 1 AND m.client_active = 1 AND m.client_inactive = 1 AND m.client_lost = 1),
    (10, 'Clientes y servicios', 'Clients linked to company and contact', '4', m.clients_linked::text, m.clients_linked = 4),
    (10, 'Clientes y servicios', 'Services created', '5', m.services::text, m.services = 5),
    (10, 'Clientes y servicios', 'Active services', '4', m.services_active::text, m.services_active = 4),
    (10, 'Clientes y servicios', 'Inactive services', '1', m.services_inactive::text, m.services_inactive = 1),
    (10, 'Clientes y servicios', 'Pricing model distribution', 'HOURLY=2, FIXED=2, RECURRING=1',
        'HOURLY=' || m.services_hourly || ', FIXED=' || m.services_fixed || ', RECURRING=' || m.services_recurring,
        m.services_hourly = 2 AND m.services_fixed = 2 AND m.services_recurring = 1),
    (10, 'Clientes y servicios', 'Positive service rates', '5', m.services_positive_rate::text, m.services_positive_rate = 5),
    (10, 'Clientes y servicios', 'Service currency count', '2', m.service_currencies::text, m.service_currencies = 2),

    -- Day 11: proposals
    (11, 'Propuestas', 'Proposals created', '4', m.proposals::text, m.proposals = 4),
    (11, 'Propuestas', 'Proposal items created', '6', m.items::text, m.items = 6),
    (11, 'Propuestas', 'Subtotal equals sum of items', '0', m.proposal_subtotal_mismatches::text, m.proposal_subtotal_mismatches = 0),
    (11, 'Propuestas', 'Total equals subtotal', '0', m.proposal_total_mismatches::text, m.proposal_total_mismatches = 0),
    (11, 'Propuestas', 'Amount equals quantity x unit price', '0', m.item_amount_mismatches::text, m.item_amount_mismatches = 0),
    (11, 'Propuestas', 'Proposal status distribution', 'ACCEPTED=2, REJECTED=1, SENT=1',
        'ACCEPTED=' || m.proposal_accepted || ', REJECTED=' || m.proposal_rejected || ', SENT=' || m.proposal_sent,
        m.proposal_accepted = 2 AND m.proposal_rejected = 1 AND m.proposal_sent = 1),
    (11, 'Propuestas', 'Accepted revenue in EUR', '3700.00', m.accepted_eur::text, m.accepted_eur = 3700.00),
    (11, 'Propuestas', 'Accepted revenue in USD', '3500.00', m.accepted_usd::text, m.accepted_usd = 3500.00),
    (11, 'Propuestas', 'Proposal history records', '9', m.history::text, m.history = 9),
    (11, 'Propuestas', 'Proposals with history', '4', m.proposals_with_history::text, m.proposals_with_history = 4),
    (11, 'Propuestas', 'History change types', 'CREATED=4, UPDATED=5',
        'CREATED=' || m.history_created || ', UPDATED=' || m.history_updated,
        m.history_created = 4 AND m.history_updated = 5),
    (11, 'Propuestas', 'Latest history total matches proposal total', '0', m.history_latest_mismatches::text, m.history_latest_mismatches = 0),

    -- Day 12: subscriptions
    (12, 'Suscripciones', 'Subscriptions created', '6', m.subscriptions::text, m.subscriptions = 6),
    (12, 'Suscripciones', 'Active subscriptions', '5', m.subs_active::text, m.subs_active = 5),
    (12, 'Suscripciones', 'Cancelled subscriptions', '1', m.subs_cancelled::text, m.subs_cancelled = 1),
    (12, 'Suscripciones', 'Active subscription currencies', '2', m.subs_currencies::text, m.subs_currencies = 2),
    (12, 'Suscripciones', 'Active periodicity distribution', 'MONTHLY=3, QUARTERLY=1, YEARLY=1',
        'MONTHLY=' || m.subs_monthly || ', QUARTERLY=' || m.subs_quarterly || ', YEARLY=' || m.subs_yearly,
        m.subs_monthly = 3 AND m.subs_quarterly = 1 AND m.subs_yearly = 1),
    (12, 'Suscripciones', 'Monthly subscription cost in EUR', '160.00', m.monthly_eur::text, m.monthly_eur = 160.00),
    (12, 'Suscripciones', 'Monthly subscription cost in USD', '3560.00', m.monthly_usd::text, m.monthly_usd = 3560.00),
    (12, 'Suscripciones', 'Annual subscription cost in EUR', '1920.00', m.annual_eur::text, m.annual_eur = 1920.00),
    (12, 'Suscripciones', 'Annual subscription cost in USD', '42720.00', m.annual_usd::text, m.annual_usd = 42720.00),
    (12, 'Suscripciones', 'Renewals within 30 days', '4', m.subs_upcoming::text, m.subs_upcoming = 4),
    (12, 'Suscripciones', 'Active subscriptions with renewal date', '5', m.subs_active_with_renewal::text, m.subs_active_with_renewal = 5),

    -- Day 13: tasks and audit
    (13, 'Tareas y auditoría', 'Tasks created', '8', m.tasks::text, m.tasks = 8),
    (13, 'Tareas y auditoría', 'Task status distribution', 'TODO=4, IN_PROGRESS=1, DONE=2, CANCELLED=1',
        'TODO=' || m.task_todo || ', IN_PROGRESS=' || m.task_in_progress || ', DONE=' || m.task_done || ', CANCELLED=' || m.task_cancelled,
        m.task_todo = 4 AND m.task_in_progress = 1 AND m.task_done = 2 AND m.task_cancelled = 1),
    (13, 'Tareas y auditoría', 'Overdue tasks', '2', m.task_overdue::text, m.task_overdue = 2),
    (13, 'Tareas y auditoría', 'Tasks linked to other entities', '7', m.task_linked::text, m.task_linked = 7),
    (13, 'Tareas y auditoría', 'Manual tasks without link', '1', m.task_manual::text, m.task_manual = 1),
    (13, 'Tareas y auditoría', 'Audit events created', '10', m.audit_events::text, m.audit_events = 10),
    (13, 'Tareas y auditoría', 'Audited entity types', '4', m.audit_entity_types::text, m.audit_entity_types = 4),
    (13, 'Tareas y auditoría', 'Audited actions', '2', m.audit_actions::text, m.audit_actions = 2),
    (13, 'Tareas y auditoría', 'Audit events without an existing entity', '0', m.audit_orphans::text, m.audit_orphans = 0),
    (13, 'Tareas y auditoría', 'Audit events with JSON details', '10', m.audit_with_details::text, m.audit_with_details = 10),

    -- Day 14: dashboard
    (14, 'Dashboard', 'clients.total', '4', m.clients::text, m.clients = 4),
    (14, 'Dashboard', 'clients.potential', '1', m.client_potencial::text, m.client_potencial = 1),
    (14, 'Dashboard', 'clients.active', '1', m.client_active::text, m.client_active = 1),
    (14, 'Dashboard', 'services.total', '5', m.services::text, m.services = 5),
    (14, 'Dashboard', 'services.active', '4', m.services_active::text, m.services_active = 4),
    (14, 'Dashboard', 'proposals.total', '4', m.proposals::text, m.proposals = 4),
    (14, 'Dashboard', 'acceptedByCurrency has one entry per currency', '2', m.accepted_currencies::text, m.accepted_currencies = 2),
    (14, 'Dashboard', 'subscriptions.active', '5', m.subs_active::text, m.subs_active = 5),
    (14, 'Dashboard', 'monthlyByCurrency has one entry per currency', '2', m.subs_currencies::text, m.subs_currencies = 2),
    (14, 'Dashboard', 'annualByCurrency has one entry per currency', '2', m.subs_currencies::text, m.subs_currencies = 2),
    (14, 'Dashboard', 'subscriptions.upcomingRenewals', '4', m.subs_upcoming::text, m.subs_upcoming = 4),
    (14, 'Dashboard', 'tasks.total', '8', m.tasks::text, m.tasks = 8),
    (14, 'Dashboard', 'tasks.pending', '4', m.task_todo::text, m.task_todo = 4),
    (14, 'Dashboard', 'tasks.inProgress', '1', m.task_in_progress::text, m.task_in_progress = 1),
    (14, 'Dashboard', 'tasks.done', '2', m.task_done::text, m.task_done = 2),
    (14, 'Dashboard', 'tasks.overdue', '2', m.task_overdue::text, m.task_overdue = 2),
    (14, 'Dashboard', 'Margin in EUR', '3540.00', m.margin_eur::text, m.margin_eur = 3540.00),
    (14, 'Dashboard', 'Margin in USD', '-60.00', m.margin_usd::text, m.margin_usd = -60.00),
    (14, 'Dashboard', 'Margins grouped by currency', '2', m.margin_currencies::text, m.margin_currencies = 2),

    -- Manual checks that require the browser.
    (14, 'Dashboard', 'Dashboard page shows the integrated metrics', 'Visible in UI', 'Pending manual verification', NULL::boolean),
    (14, 'Dashboard', 'Module navigation works with the integrated data', 'Visible in UI', 'Pending manual verification', NULL::boolean)
) AS v(day, module, case_name, expected, actual, passed);

SELECT day, module, case_name, expected, actual,
       CASE WHEN passed IS NULL THEN 'Pendiente' WHEN passed THEN 'Cumple' ELSE 'Falla' END AS result
FROM option_a_checks
ORDER BY day, module, case_name;

SELECT day,
       count(*) AS pruebas,
       count(*) FILTER (WHERE passed) AS cumplen,
       count(*) FILTER (WHERE passed IS FALSE) AS fallan,
       count(*) FILTER (WHERE passed IS NULL) AS pendientes,
       round(100.0 * count(*) FILTER (WHERE passed)
             / NULLIF(count(*) FILTER (WHERE passed IS NOT NULL), 0), 1) AS cobertura_ejecutada_pct,
       round(100.0 * count(*) FILTER (WHERE passed) / count(*), 1) AS cobertura_total_pct
FROM option_a_checks
GROUP BY day
ORDER BY day;

SELECT count(*) AS pruebas,
       count(*) FILTER (WHERE passed) AS cumplen,
       count(*) FILTER (WHERE passed IS FALSE) AS fallan,
       count(*) FILTER (WHERE passed IS NULL) AS pendientes,
       round(100.0 * count(*) FILTER (WHERE passed)
             / NULLIF(count(*) FILTER (WHERE passed IS NOT NULL), 0), 1) AS cobertura_ejecutada_pct,
       round(100.0 * count(*) FILTER (WHERE passed) / count(*), 1) AS cobertura_total_pct
FROM option_a_checks;

COMMIT;
