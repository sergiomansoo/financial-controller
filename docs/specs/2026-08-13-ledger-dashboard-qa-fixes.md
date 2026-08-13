# Ledger Dashboard QA Fix Specification

**Status:** Required before release
**Source:** QA review after commits `4f91fe9` and `58ba862`

## Purpose

Correct the dashboard filtering semantics, the explicit ledger visual constraints, and missing user-facing failure handling found during final QA.

## Backend: filtered dashboard aggregates

- `DashboardFilter` must affect every aggregate used by `/api/v1/dashboard`, not only `byCategory`.
- For `filter=income`, `totals.income` is the selected income amount, `totals.expense` is `0`, and `totals.balance` equals the selected income amount.
- For `filter=expense`, `totals.income` is `0`, `totals.expense` is the selected expense amount, and `totals.balance` is the negated selected expense amount.
- For `filter=both`, preserve current income/expense/balance behavior and keep investments visible only in unfiltered category/transaction views.
- The six-month evolution must apply the selected filter: suppressed series is zero; `both` retains both existing series.
- Add red-green integration coverage for `both`, `income`, and `expense` in `DashboardControllerIT`.

## Frontend: ledger system and error recovery

- Replace `.ledger-skeleton` gradient usage with a solid-token skeleton that may use opacity animation only; honor `prefers-reduced-motion`.
- Cards/panels must use exactly `10px` radius and buttons, inputs, selects, and segmented controls exactly `6px`.
- In collapsed sidebar mode, navigation labels require accessible tooltips after 300ms hover/focus delay. Add an interaction test.
- Category and rule deletion must catch API failures and show the returned API message in an alert. Do not leave unhandled promise rejections.
- Add route coverage for all protected routes: `/dashboard`, `/transacoes`, `/importar`, `/categorias`, `/metas`, `/configuracoes` and the legacy `/transactions` redirect.
- Add workflow coverage for preview -> explicit confirmation -> upload, plus page-level category, goal, and settings interactions.

## Acceptance

1. Focused tests fail before each correction and pass after it.
2. `backend\mvnw.cmd test` passes.
3. `frontend\npm test`, `frontend\npm run build`, and `git diff --check` pass.
4. No gradients, non-token positive/negative colors, or wrong radii remain in ledger components.
5. The filter changes KPI totals, category data, monthly series, and transaction rows consistently.
6. Each correction is committed using an English Conventional Commit.
