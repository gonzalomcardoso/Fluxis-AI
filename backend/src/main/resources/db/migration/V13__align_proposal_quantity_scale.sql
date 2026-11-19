ALTER TABLE proposal_items
    ALTER COLUMN quantity TYPE NUMERIC(19, 2)
    USING GREATEST(ROUND(quantity, 2), 0.01);