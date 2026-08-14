# Mini-spec — Dashboard category highlights by movement type

## Finding

The dashboard response only exposes the legacy expense highlight. This makes
the `income` filter lack an income highlight and makes `both` unable to show
independent income and expense highlights, contrary to the insights contract.

## Required API behavior

For `GET /api/v1/dashboard?month=YYYY-MM&filter=...`:

- `filter=expense`: return the largest expense highlight; the income highlight
  is absent/null.
- `filter=income`: return the largest income highlight; the expense highlight
  is absent/null.
- `filter=both`: return both independently.
- A highlight is an additive structured object with `categoryId`,
  `categoryName`, and positive `amount`; `INVESTMENT` never competes in either
  highlight.
- Preserve legacy `largestExpenseCategory` and `largestExpenseAmount` for
  frontend compatibility until its dedicated follow-up consumes the structured
  values.
- Every aggregate remains user-scoped and expense amounts are absolute.

## Tests and verification

- Add RED/GREEN integration coverage for `income`, `expense`, and `both`,
  including independent winners and no cross-user data.
- Run focused dashboard tests, `mvnw clean test`, and `git diff --check`.
- Commit only the backend change with an English Conventional Commit.
