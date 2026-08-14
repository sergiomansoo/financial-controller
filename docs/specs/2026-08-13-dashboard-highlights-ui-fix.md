# Mini-spec — Render typed dashboard highlights

## Finding

The API now exposes `totals.largestIncome` and `totals.largestExpense` by
movement filter. The dashboard UI still renders only the legacy expense
highlight, so the income mode does not present its highest category.

## Required behavior

- Extend frontend dashboard types for additive `largestIncome` and
  `largestExpense` structured values (`categoryId`, `categoryName`, `amount`).
- In `both`, render the expense highlight required by the existing KPI layout
  and make the income highlight available in the dashboard insights area.
- In `income`, render the highest income category and amount; do not render it
  as an expense.
- In `expense`, render the highest expense category and positive amount.
- Retain a safe fallback to legacy expense fields while older API responses are
  in use.
- Keep labels Portuguese and preserve the ledger visual system.

## Regression coverage

- Add tests for income, expense, and both dashboard payloads.
- Run focused dashboard tests, full frontend tests, production build, and
  `git diff --check`.
