# Mini-spec — Complete Savings Goals UI Workflow

## Finding

`/metas` creates and lists savings goals but does not expose the existing API operations to save monthly planned/saved amounts or delete a goal. This leaves the required savings-goal workflow incomplete.

## Scope

Modify frontend only:

- `frontend/src/pages/GoalsPage.tsx`
- `frontend/src/pages/GoalsPage.test.tsx`
- Supporting frontend API/types only if required by the existing endpoint contract.

Do not modify backend contracts.

## Required behavior

For each savings goal displayed at `/metas`:

1. Show inputs for the selected month’s `plannedAmount` and `savedAmount`.
2. Save them through `PUT /api/v1/savings-goals/{goalId}/months/{YYYY-MM}` using `{ plannedAmount, savedAmount }`.
3. Replace the rendered goal with the returned response immediately, including monthly and overall percentages.
4. Provide an explicit delete control which calls `DELETE /api/v1/savings-goals/{goalId}` and removes only the returned/deleted goal from the page.
5. Present a Portuguese inline error on failed save/delete; do not clear current values on failure.
6. Require a target date when creating a savings goal, because the completion spec defines name, target amount, and target date as the creation contract.
7. Catch a rejected `createSavingsGoal` request and render a Portuguese inline alert while retaining the form values.

## Regression tests

- Saving planned/saved values sends the selected month and exact numeric body, then renders updated progress values.
- Deleting a goal calls the correct endpoint and removes that goal from the list.
- A rejected save/delete keeps the goal visible and renders an alert.
- Creating without a target date is blocked by native/form validation; a rejected creation request renders a Portuguese alert and does not append a goal.

## Verification

## Follow-up compilation fix

The first implementation attempt placed `await createSavingsGoal(...)` inside a React state-updater callback, which is invalid TypeScript. Resolve the API promise before calling `setGoals`, then append the resolved goal synchronously. Re-run the focused goals test, full frontend suite and production build before committing.

Run `npm test`, `npm run build`, and `git diff --check`.
