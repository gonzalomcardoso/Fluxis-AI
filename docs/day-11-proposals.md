# Day 11 — Commercial proposals

Day 11 adds commercial proposals related to a client and one or more professional services.

## Scope completed

- `proposals` and `proposal_items` Flyway migration V7.
- Proposal statuses: `DRAFT`, `PENDING_REVIEW`, `SENT`, `ACCEPTED`, `REJECTED` and `EXPIRED`.
- Decimal amounts using `NUMERIC(19,4)` and `BigDecimal`.
- Backend CRUD at `/api/proposals`.
- Validation that the client and services belong to the authenticated user.
- Total calculation from proposal items.
- Frontend route `/propuestas` with client/service selection and item total display.
- Frontend schema tests for decimal totals and status validation.

The API does not mark a proposal as sent merely because a future email draft is created; email integration remains controlled by the approval flow.
