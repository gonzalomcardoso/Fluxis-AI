# Day 12 — Subscriptions and recurring expenses

- Subscriptions use `NUMERIC(19,2)` amounts, so `9.22` means nine euros and twenty-two cents.
- Monthly and annual estimates are calculated by periodicity.
- Summaries are grouped by ISO currency and never combine different currencies.
- Upcoming renewals are returned for the next 30 days.
- API CRUD: `/api/subscriptions`.
- Summary: `/api/subscriptions/summary`.
- Frontend: `/suscripciones`.
