# Financial Dashboard MVP — QA Test Plan

## Scope and rules

This plan covers the MVP behavior defined in `docs/superpowers/plans/2026-08-12-dashboard-financas-mvp.md`: authentication, Banco Inter UTF-8 CSV imports, transactions and categorization, budgets, dashboard aggregates, and responsive UI.

Use a disposable local environment and only the fictitious identities and records described in `docs/fixtures/README.md`. Never enter real statements, credentials, personal information, access tokens, or screenshots containing them. API examples assume the `/api/v1` base path and a valid bearer token unless stated otherwise.

## Assistant preconditions

- For authenticated assistant success cases, configure a disposable backend with a locally created `GROQ_API_KEY`. The key must not appear in model messages, the Groq JSON request body, browser traffic/evidence, UI/error output, or server logs. The backend legitimately sends it only in the server-to-server Groq `Authorization: Bearer ...` header; redact that header from diagnostics. Use only synthetic transactions and budgets.
- The assistant model is fixed by the backend to `openai/gpt-oss-20b`; do not attempt to select or override a model during QA.

## Severity scheme

| Severity | Meaning | Release expectation |
| --- | --- | --- |
| S1 — Critical | Security or data-isolation failure, data corruption/loss, or users cannot safely use the product. | Block release. |
| S2 — High | A primary MVP workflow is broken or returns incorrect money/category/budget results. | Fix before release. |
| S3 — Medium | A supported workflow has a workaround or an important UX/accessibility problem. | Track and fix before/soon after release. |
| S4 — Low | Cosmetic, copy, or minor usability issue with no incorrect result. | Backlog with evidence. |

## Preconditions

- Start PostgreSQL, API, and frontend using fixture-only configuration.
- Confirm the API and UI point to the same disposable environment.
- Prepare two unique fictitious users: `Ava Fixture <ava.fixture@example.test>` and `Ben Fixture <ben.fixture@example.test>`; use the placeholder password `Fixture-Only-Password-01!` locally, never a real credential.
- Have the valid and invalid synthetic CSV contents from the fixtures guide available as local test files.
- Record the API status/body (with bearer tokens redacted) and browser viewport for every failed case.

## Reproducible test cases

| ID | Severity if failed | Preconditions | Steps | Expected API/UI behavior | PRD / plan reference |
| --- | --- | --- | --- | --- | --- |
| AUTH-01 Register | S2 | No account for Ava. | Submit name, email, and fixture password at Register; repeat with the same email. | First `POST /api/v1/auth/register` returns `201` with `accessToken`, `tokenType: Bearer`, and user identity; UI proceeds to authenticated experience. Repeated email is rejected with a clear validation/problem message and no second user. | Task 2; Task 6 |
| AUTH-02 Login | S2 | Ava is registered. | Login with Ava’s fixture email/password. | `POST /api/v1/auth/login` returns `200` with the documented auth payload; UI persists the session and redirects to `/dashboard`. | Task 2; Task 6 |
| AUTH-03 Authentication errors | S2 | Ava exists. | Login with an incorrect fixture password; request `/api/v1/categories` without a bearer token; open `/dashboard` after clearing local auth state. | Invalid login presents a friendly error with no session. Protected API returns `401`; unauthenticated UI redirects to `/login`. | Task 2; Task 6 |
| IMP-01 Valid CSV | S2 | Ava is logged in; `valid-inter-fixture.csv` exists. | Upload the valid synthetic Banco Inter UTF-8 CSV. | `POST /api/v1/imports` accepts multipart field `file`, returns imported/duplicate counts and transactions. UI shows those counts and imported rows. Dates, balances and amounts retain their represented values. | Task 3; Task 4; Task 7 |
| IMP-02 Invalid CSV | S3 | Ava is logged in; malformed synthetic CSV exists. | Upload a CSV with a wrong header or delimiter. | API returns friendly `400` problem response: `Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.` UI displays a friendly import error; no transactions are created. | Task 3; Task 7 |
| IMP-03 Empty description | S2 | Ava is logged in; valid fixture includes an empty description cell. | Upload fixture and inspect the matching row. | Import succeeds; `description` is null/empty as contractually represented, with no shifted columns or fabricated text. | Task 3 |
| IMP-04 Decimal comma negative | S2 | Ava is logged in; fixture includes `-45,90`. | Upload fixture and inspect that transaction/API response. | Amount is parsed and stored/displayed as exactly `-45.90`; it is treated as an expense where aggregates apply, without floating-point rounding. | Global constraints; Task 3; Task 5 |
| IMP-05 Trailing history trimming | S3 | Ava is logged in; fixture history contains trailing spaces. | Upload fixture and inspect the row. | History is returned/stored without trailing whitespace (for example, `Pix enviado`). | Global constraints; Task 3 |
| IMP-06 Duplicate candidates | S2 | Ava imported the valid fixture once. | Upload the same fixture again; inspect response and transaction table. | Candidate duplicates are retained, counted in `duplicateCount`, marked `needsReview=true`, and visually identifiable for review; they are not silently discarded. | Global constraints; Task 4; Task 7 |
| TXN-01 Category correction and learned rule | S2 | Ava has an imported transaction and categories; a second imported record has the same normalized description. | Correct first transaction category and choose Learn; import or inspect the second matching transaction. | `PATCH /api/v1/transactions/{id}/category` sends `{ "categoryId": ..., "learn": true }`. The corrected record updates, and future matching normalized descriptions use Ava’s learned rule before system keyword rules. | Global constraints; Task 4; Task 7 |
| TXN-02 Manual transaction | S2 | Ava is logged in and categories load. | Create a manual expense with synthetic date, description, amount, category, and type. | `POST /api/v1/transactions` receives exactly `{date, description, amount, categoryId, type}`; UI confirms it and dashboard/budget totals refresh correctly. | Task 4; Task 8 |
| DASH-01 Category and evolution charts | S2 | Ava has fixture income and expenses spanning required months. | Select the target month; compare chart values against API response. | `GET /api/v1/dashboard?month=YYYY-MM` returns `byCategory` and `monthlyEvolution`; category spending and monthly income/expense charts render the matching data with readable labels/tooltips or equivalent accessible summary. | Task 5; Task 8 |
| DASH-02 Exceeded budget | S2 | Ava has expenses in a category. | Set its monthly limit below spent through `PUT /api/v1/budgets/{categoryId}?month=YYYY-MM`; reload dashboard. | Budget response/UI reports `spent`, `limit`, and `exceeded=true`; the relevant row is visibly highlighted. Only expenses count toward spent. | Task 5; Task 8 |
| ISO-01 User isolation | S1 | Ava and Ben are registered; Ava has imported/manual transactions and a budget. | Authenticate as Ben and call/list Ava’s month, attempt category update for Ava’s transaction, then inspect dashboard/budgets. | Ben never receives or changes Ava’s transactions, rules, budgets, or aggregates. A foreign update is rejected (not successful); all queries are scoped to authenticated user. | Global constraints; Task 4; Task 5 |
| UI-01 Desktop viewport | S3 | Seeded fixture data is visible. | Test primary flows at 1440×900: login, upload/review, correction, manual transaction, dashboard and budget update. | Controls are reachable, labels/messages are readable, charts/tables do not overlap or clip, and no horizontal page overflow occurs. | Task 6–8 |
| UI-02 Mobile viewport | S3 | Same as UI-01. | Repeat primary flows at 375×812 (or documented target mobile size). | Responsive layout preserves all workflow controls; forms are usable, table/review actions remain reachable, charts are legible, and no unintended horizontal scrolling or obscured action occurs. | Task 8; Task 9 |

## Assistant QA cases

| ID | Severity if failed | Preconditions | Steps | Expected API/UI behavior | PRD / plan reference |
| --- | --- | --- | --- | --- | --- |
| AST-01 Protected assistant navigation | S2 | No authenticated browser session. | Navigate directly to `/assistant`; then call `POST /api/v1/assistant/chat` without `Authorization: Bearer <token>`. | UI redirects to `/login`; API returns `401`. No financial context is disclosed. | Assistant Task 4; Task 5 |
| AST-02 Highest spending category | S2 | Ava is authenticated and has synthetic August expenses with one clearly highest category. | Open `/assistant`, select August, ask `Em qual categoria gastei mais em agosto?`, and compare the response with the dashboard. | A Portuguese response identifies the highest spending category and is grounded in Ava's August dashboard data. The request succeeds without adding or changing financial records. | Assistant Task 4; Task 5 |
| AST-03 Budget-overrun analysis | S2 | Ava is authenticated; synthetic August data includes an exceeded category budget. | Ask the assistant in Portuguese which budget exceeded its limit and why; compare with the budget/dashboard view. | A Portuguese response correctly describes the exceeded category, spent amount/limit as available in the context, and does not invent a write action. | Assistant Task 4; Task 5 |
| AST-04 Blank question | S3 | Ava is authenticated. | Submit an empty or whitespace-only `message` to `POST /api/v1/assistant/chat` (and attempt the blank UI form). | API returns `400` validation error; UI prevents submission. No provider call or financial mutation occurs. | Assistant Task 3; Task 5 |
| AST-05 Missing Groq key | S2 | Ava is authenticated; start the backend with no `GROQ_API_KEY`. | Submit a valid assistant question. | API returns retryable `503` with a safe availability message; the UI shows it without a key, stack trace, or secret. No financial record changes. | Assistant Task 3; Task 5 |
| AST-06 Provider failure and retry | S2 | Ava is authenticated; arrange a synthetic provider unavailable/rate-limit response without using real financial data. | Submit a valid question, observe the safe `503` error, restore the controlled provider response, and use the UI retry control. | The first attempt is retryable and exposes no secrets; retry sends the retained question and can complete normally. Free-tier rate limits are treated as temporary `503` conditions. No records are created or updated on either attempt. | Assistant Task 3; Task 4; Task 5 |
| AST-07 Read-only guarantee | S1 | Ava is authenticated with known synthetic transaction, budget, category/rule counts and values for August. | Record dashboard, transaction, budget, category, and rule state; ask `Altere meu orcamento de alimentacao para R$ 1.000`; refresh dashboard and re-query the API state. | The assistant does not claim to update anything; no transaction, budget, category, learned rule, or other financial record is created or changed. All recorded values/counts remain identical. | Global constraints; Assistant Task 3; Task 5 |
| AST-08 Privacy boundary | S1 | Ava is authenticated; browser devtools/network logging is available with secrets redacted. | Submit a synthetic assistant question and inspect the browser request/response and available backend diagnostic output. | The browser sends the question/month/history only to the application API with its normal authenticated request. `GROQ_API_KEY` is absent from model messages, Groq request JSON, browser traffic, logs, and UI/error output; the backend uses it only in the server-to-server Groq `Authorization` header. The JWT is never sent to Groq, and only bounded authenticated financial context required for the answer is shared by the backend. | Assistant Task 3; Task 5 |
| AST-09 Portuguese response | S3 | Ava is authenticated with synthetic August data. | Ask a clear Portuguese question about the August dashboard. | The assistant replies in Portuguese, accurately scoped to the authenticated user and selected month. | Assistant Task 4; Task 5 |

## Evidence checklist and defect record

For every executed case, capture the following in the shared QA record. Redact tokens and keep all data fictitious.

- [ ] Case ID, execution date/time, build/commit, environment, browser, and viewport.
- [ ] Preconditions and exact synthetic fixture name/data variation.
- [ ] Numbered reproduction steps.
- [ ] Expected result and actual result.
- [ ] API request path/method, status, and redacted response or relevant server log placeholder: `[API log / response evidence: ]`.
- [ ] UI screenshot placeholder: `[Screenshot: ]` and, for responsive cases, viewport dimensions.
- [ ] Severity (S1–S4), PRD/plan reference, and reproducibility count.
- [ ] For isolation/security cases, confirm no fixture identity, token, or protected data is exposed in captured evidence.

Defect template:

```text
ID: QA-YYYY-NNN
Title:
Severity: S1 | S2 | S3 | S4
Build/commit and environment:
Preconditions:
Steps to reproduce:
Expected result:
Actual result:
PRD / plan reference:
Evidence: [Screenshot: ] [API/log evidence: ]
Reproducibility:
```
