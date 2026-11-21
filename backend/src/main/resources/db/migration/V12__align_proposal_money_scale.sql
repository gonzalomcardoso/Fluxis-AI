ALTER TABLE proposals
    ALTER COLUMN subtotal TYPE NUMERIC(19, 2) USING ROUND(subtotal, 2),
    ALTER COLUMN total TYPE NUMERIC(19, 2) USING ROUND(total, 2);

ALTER TABLE proposal_items
    ALTER COLUMN amount TYPE NUMERIC(19, 2) USING ROUND(amount, 2);

ALTER TABLE proposal_history
    ALTER COLUMN subtotal TYPE NUMERIC(19, 2) USING ROUND(subtotal, 2),
    ALTER COLUMN total TYPE NUMERIC(19, 2) USING ROUND(total, 2);