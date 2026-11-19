-- Option A cleanup: removes only the records created by option-a-integrated.sql.
-- Run only against the test database, and only when the Option A dataset must
-- be recreated from scratch. Option B and Option C rows are not touched.
--
-- The script uses the same markers and dependency order as the loader.

BEGIN;

DELETE FROM audit_events
WHERE details->>'source' = 'option-a-integrated';

DELETE FROM tasks
WHERE origin = 'OPTION_A';

DELETE FROM subscriptions
WHERE category = 'OPTION_A';

-- Proposal items and proposal history cascade from proposals.
DELETE FROM proposals
WHERE description = 'Option A integrated scenario.';

DELETE FROM services
WHERE notes = 'Option A integrated scenario.';

DELETE FROM clients
WHERE origin = 'OPTION_A';

-- Interviews and follow-ups cascade from job applications.
DELETE FROM job_applications
WHERE notes = 'Option A integrated scenario.';

DELETE FROM job_opportunities
WHERE source = 'Option A integrated scenario';

DELETE FROM contacts
WHERE origin = 'OPTION_A';

DELETE FROM companies
WHERE notes = 'Option A integrated scenario.';

COMMIT;
