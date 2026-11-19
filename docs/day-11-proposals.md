# Day 11 — Commercial proposals

Day 11 adds commercial proposals related to a client and one or more professional services.

## Scope completed

- `proposals` and `proposal_items` Flyway migration V7, with V12 aligning monetary totals to two decimals.
- Proposal statuses: `DRAFT`, `PENDING_REVIEW`, `SENT`, `ACCEPTED`, `REJECTED` and `EXPIRED`.
- All monetary values, including proposal unit prices, totals, and line amounts, use two decimal places. Quantities also use two decimal places.
- Backend CRUD at `/api/proposals`.
- Validation that the client and services belong to the authenticated user.
- Total calculation from proposal items.
- Proposal change history at `/api/proposals/{id}/history`.
- History snapshots for creation and updates, including status and totals.
- Frontend route `/propuestas` with client/service selection and item total display.
- Frontend schema tests for decimal totals and status validation.

The API does not mark a proposal as sent merely because a future email draft is created; email integration remains controlled by the approval flow.
