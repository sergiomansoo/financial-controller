# Mini-spec — Goals loading and expired session handling

## Finding

`GoalsPage` leaves `budgets` as `null` when either initial request rejects.
An expired/invalid token therefore leaves the user on an indefinite
“Carregando metas…” state with no recovery path.

## Required behavior

- Initial loading for monthly budgets and savings goals must always settle.
- On a 401/403 API response, clear the invalid local session and route the user
  to login with a Portuguese message explaining that the session expired.
- On other load failures, show a Portuguese inline error and a retry action;
  never retain an infinite loading state.
- Preserve the normal loaded page when either list is empty.

## Verification

- Add regression tests for authenticated success, unauthorized loading and a
  generic API failure/retry.
- Run focused GoalsPage tests, full frontend tests, production build, and
  `git diff --check`.
