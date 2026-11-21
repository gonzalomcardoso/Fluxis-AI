ALTER TABLE services
    ALTER COLUMN rate TYPE NUMERIC(19, 2)
    USING GREATEST(ROUND(rate, 2), 0.01);

ALTER TABLE proposal_items
    ALTER COLUMN unit_price TYPE NUMERIC(19, 2)
    USING GREATEST(ROUND(unit_price, 2), 0.00);