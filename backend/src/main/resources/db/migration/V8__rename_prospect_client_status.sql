ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_status_check;
UPDATE clients SET status = 'POTENCIAL' WHERE status = 'PROSPECT';
ALTER TABLE clients ADD CONSTRAINT clients_status_check
    CHECK (status IN ('POTENCIAL', 'ACTIVE', 'INACTIVE', 'LOST'));
