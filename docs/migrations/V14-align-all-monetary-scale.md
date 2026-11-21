# Migration V14 - Align all monetary values to two decimals

- **Migration file:** `backend/src/main/resources/db/migration/V14__align_all_monetary_scale.sql`
- **Status:** Prepared, not applied
- **Required environment order:** DEV -> TEST -> PROD
- **Approval required before execution:** Yes

## Purpose

Every monetary value stored by Fluxis AI must use two decimal places. Earlier
migrations aligned proposal totals, proposal line amounts, quantities, and
subscriptions. This migration aligns the remaining monetary columns:

- `services.rate`
- `proposal_items.unit_price`

## Affected columns

| Table | Column | Previous type | New type | Transformation |
|---|---|---|---|---|
| `services` | `rate` | `NUMERIC(19,4)` | `NUMERIC(19,2)` | `GREATEST(ROUND(rate, 2), 0.01)` |
| `proposal_items` | `unit_price` | `NUMERIC(19,4)` | `NUMERIC(19,2)` | `GREATEST(ROUND(unit_price, 2), 0.00)` |

The minimum rate is preserved as `0.01` because `services.rate` has a
`rate > 0` constraint. Unit prices remain allowed at zero.

## Data impact

Existing values with more than two decimal places are rounded. Values whose
rate rounds below `0.01` are raised to `0.01` to preserve the existing check
constraint. No rows are added or deleted.

Run these read-only checks before applying the migration:

```sql
SELECT count(*) AS service_rates_affected
FROM services
WHERE rate <> ROUND(rate, 2);

SELECT count(*) AS proposal_unit_prices_affected
FROM proposal_items
WHERE unit_price <> ROUND(unit_price, 2);
```

## Expected Flyway result

```text
14 | align all monetary scale | t
```

## Post-migration validation

```sql
SELECT table_name, column_name, numeric_precision, numeric_scale
FROM information_schema.columns
WHERE (table_name, column_name) IN (
    ('services', 'rate'),
    ('proposal_items', 'unit_price'),
    ('proposal_items', 'amount'),
    ('proposals', 'subtotal'),
    ('proposals', 'total'),
    ('proposal_history', 'subtotal'),
    ('proposal_history', 'total'),
    ('subscriptions', 'amount')
)
ORDER BY table_name, column_name;
```

Every returned row must have `numeric_precision = 19` and `numeric_scale = 2`.

## Rollback and failure handling

This migration is intentionally forward-only. If a problem is found after it is
applied, create a new migration that restores the required schema or data. Do not
edit an applied Flyway migration. Preserve the database volume and take a backup
before applying the migration to a persistent environment.

## Application procedure

1. Obtain explicit user approval for DEV.
2. Back up the DEV database if needed.
3. Apply and validate in DEV.
4. Stop and wait for user approval.
5. Repeat for TEST.
6. Stop and wait for user approval.
7. Repeat for PROD.