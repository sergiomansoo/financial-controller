# Mini-spec — Total spent for transaction filters

## Goal

The Transactions page must display **Total gasto no período**, not a signed
net total. It updates whenever the user changes category or the selected
period (Month, or complete From/To range).

## Contract

- `GET /api/v1/transactions/total` accepts the established user-scoped
  period and optional `categoryId` filters.
- Its response adds `totalSpent`: the positive absolute sum of matching
  `EXPENSE` transactions only. Income and investment never offset it.
- Keep existing `total` only if required for compatibility; the UI renders
  `totalSpent` under the Portuguese label **Total gasto no período**.
- A category with no expenses returns `R$ 0,00`.
- Month and From/To remain mutually exclusive; incomplete or invalid ranges
  do not request a total.

## Verification

- Backend tests for user scope, negative expense normalization, category and
  date range filters, and income exclusion.
- Frontend tests that category and complete range update the displayed total;
  incomplete range shows guidance instead of loading.
