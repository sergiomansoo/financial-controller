# Spec — Keyword reclassification and goals layout polish

## Goals page

- Primary/secondary/destructive actions must use the established ledger button
  variants, never unstyled transparent browser-like controls.
- In each monthly spending-limit row, the editable limit and Save action are
  grouped on the right on desktop; mobile may stack them after the progress bar.

## Categories and rules feedback

- Successful category or keyword-rule creation must not display an error.
- Errors are shown only for rejected API operations, using the returned
  Portuguese message and preserving inputs.

## Apply a keyword rule to existing transactions

- Add an explicit action for a saved rule, such as **Atualizar transações**.
- It reclassifies all accessible transactions whose normalized
  description/history starts with the rule keyword, setting the rule category.
- The operation is user-scoped, set-based/batched in the database (not one API
  request per transaction), idempotent, and returns the changed count.
- Confirm before applying; on success show Portuguese feedback with the count and
  refresh affected transaction/dashboard data on subsequent fetches.

## Verification

- Backend tests cover prefix matching, normalized accents/case, ownership,
  idempotency and changed count.
- Frontend tests cover false-success-error prevention, confirmation, action,
  success/error feedback and layout semantics.
