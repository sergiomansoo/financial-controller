# Spec — Goals session, expense highlight, and transaction filters

## Findings

1. Opening `/metas` should never invalidate a freshly authenticated session.
   A 401 must be investigated at the API/runtime boundary; the UI must not mask
   a valid-session server defect by logging the user out.
2. The live dashboard still reports `Alimentação` as largest expense when the
   larger imported spend belongs to `Outros`.
3. Transactions needs a category total and an unambiguous custom date range;
   the month filter must not conflict with `De` / `Até`.

## Acceptance criteria

- A token returned by login works for `/budgets` and `/savings-goals` during
  its validity. JWT runtime configuration is stable across normal app restarts.
- The active dashboard API returns the largest expense by absolute EXPENSE
  amount for the selected period and current user; UI displays that response.
- Transactions has a category selector, custom `De` and `Até` inputs, and a
  displayed total for the filtered rows/category.
- Choosing a custom range disables/clears the month criterion; choosing month
  clears the custom dates. Invalid ranges are rejected with Portuguese feedback.
- Query/API totals honor user scope, movement type, category and date filters.

## Verification

- Backend integration tests for login token against budgets/goals, largest
  absolute expense, category/date filtered total and user isolation.
- Frontend tests for filter exclusivity, total rendering, and valid-session
  goals loading.
- Full backend/frontend suites, production build, `git diff --check`, then
  portal validation on freshly restarted local services.
