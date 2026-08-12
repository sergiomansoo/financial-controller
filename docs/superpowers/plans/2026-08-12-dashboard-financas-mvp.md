# Dashboard Financeiro MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a personal-finance dashboard that authenticates users, imports Banco Inter CSV statements, categorizes transactions, manages monthly budgets, and visualizes spending.

**Architecture:** A monorepo contains a Spring Boot REST API in `backend/` and a Vite React client in `frontend/`. The API owns PostgreSQL persistence, JWT authentication, CSV validation/parsing, categorization, aggregate queries and user isolation; the client consumes only `/api/v1` endpoints through a typed HTTP client.

**Tech Stack:** Java 21, Spring Boot, Spring Security, JPA, Flyway, PostgreSQL, JUnit 5, React, TypeScript, Vite, Tailwind CSS, Recharts, Vitest and React Testing Library.

## Global Constraints

- Work only on branch `feature/dashboard-financas-mvp` in `C:\Users\Sergio Manso\financial-controller`.
- Never place a real bank statement, credential, JWT or personal information in source, fixtures, commits, notes or screenshots.
- Store monetary values as Java `BigDecimal` and database `numeric(19,2)`; never use binary floating point for currency.
- Protect every resource except registration and login with a validated JWT; scope database queries by the authenticated user id.
- The CSV parser accepts UTF-8 Banco Inter statements only, verifies the expected header, trims `Histórico`, accepts empty descriptions and parses decimal commas.
- Duplicate candidates are retained and marked for review; invalid formats return a friendly `400` problem response.
- Categorization order is: user learned normalized description, system keyword rule, then `Outros`.
- Implement every behavioral change test-first: observe the focused test fail, add the minimum production code, then rerun focused and relevant suite tests.
- After every cohesive, passing implementation unit create a Conventional Commit message in English, for example `feat(auth): add jwt login endpoint`.

---

### Task 1: Create the runnable monorepo foundation

**Files:**
- Create: `README.md`, `.gitignore`, `.env.example`, `docker-compose.yml`
- Create: `backend/pom.xml`, `backend/src/main/java/com/sergio/financial/FinancialControllerApplication.java`, `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/sergio/financial/FinancialControllerApplicationTests.java`
- Create: `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`, `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/src/index.css`

**Interfaces:**
- Produces `docker compose up -d db`, `cd backend && ./mvnw test`, and `cd frontend && npm test` as documented development entry points.

- [ ] **Step 1: Write failing health-context and UI smoke tests.**

```java
@SpringBootTest
class FinancialControllerApplicationTests {
  @Test void contextLoads() {}
}
```

```tsx
it('renders the finance dashboard shell', () => {
  render(<App />);
  expect(screen.getByRole('heading', { name: /financial controller/i })).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the tests and confirm they fail because the projects do not exist.**

Run: `cd backend; ./mvnw test` and `cd frontend; npm test -- --run`.

- [ ] **Step 3: Add Maven/Vite setup, the minimal application classes, Docker PostgreSQL service and environment examples.** Use Java 21, Spring Boot 3.x, PostgreSQL 16, `POSTGRES_DB=financial_controller`, an API port of `8080`, and Vite port `5173`.

- [ ] **Step 4: Run focused tests and both startup checks.**

Run: `cd backend; ./mvnw test`; `cd frontend; npm test -- --run`; `docker compose config`.

- [ ] **Step 5: Commit.**

Run: `git add README.md .gitignore .env.example docker-compose.yml backend frontend; git commit -m "chore: scaffold financial dashboard monorepo"`.

### Task 2: Implement authentication, users and default categories

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__users_and_categories.sql`
- Create: `backend/src/main/java/com/sergio/financial/auth/*`
- Create: `backend/src/main/java/com/sergio/financial/user/User.java`
- Create: `backend/src/main/java/com/sergio/financial/category/*`
- Test: `backend/src/test/java/com/sergio/financial/auth/AuthControllerIT.java`

**Interfaces:**
- Produces `POST /api/v1/auth/register` and `POST /api/v1/auth/login`, each accepting `{ "name", "email", "password" }` or login `{ "email", "password" }` and returning `{ "accessToken", "tokenType":"Bearer", "user":{ "id", "name", "email" } }`.
- Produces `GET /api/v1/categories`, returning standard categories plus the caller's categories.

- [ ] **Step 1: Write failing integration tests for registration, login, duplicate email, invalid credentials and a protected category request without a bearer token.**

```java
assertThat(postJson("/api/v1/auth/register", registration)).hasStatus(201);
assertThat(get("/api/v1/categories")).hasStatus(401);
```

- [ ] **Step 2: Run `./mvnw test -Dtest=AuthControllerIT` and observe missing endpoint failures.**

- [ ] **Step 3: Add migration, password hashing, JWT issuance/validation, exception handler and seeded system categories: Alimentação, Transporte, Mercado/Compras, Investimentos and Outros.**

- [ ] **Step 4: Rerun the focused integration test, then `./mvnw test`.**

- [ ] **Step 5: Commit.**

Run: `git add backend; git commit -m "feat(auth): add jwt authentication and categories"`.

### Task 3: Build the Banco Inter CSV parser with edge-case coverage

**Files:**
- Create: `backend/src/main/java/com/sergio/financial/importer/BancoInterCsvParser.java`
- Create: `backend/src/main/java/com/sergio/financial/importer/ParsedTransaction.java`
- Create: `backend/src/main/java/com/sergio/financial/importer/UnsupportedStatementFormatException.java`
- Create: `backend/src/test/java/com/sergio/financial/importer/BancoInterCsvParserTest.java`
- Create: `backend/src/test/resources/fixtures/inter-valid.csv`, `inter-empty-description.csv`, `inter-invalid-header.csv`

**Interfaces:**
- `List<ParsedTransaction> parse(InputStream input)` returns date, trimmed history, nullable description, amount and running balance.
- Invalid header or delimiter throws `UnsupportedStatementFormatException` with the user-facing message `Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.`

- [ ] **Step 1: Write failing parser tests for a valid negative comma decimal, empty description, trailing history whitespace, duplicate fingerprint equality and invalid header.**

```java
assertThat(rows.get(0).amount()).isEqualByComparingTo("-45.90");
assertThat(rows.get(0).history()).isEqualTo("Pix enviado");
assertThat(rows.get(0).description()).isNull();
```

- [ ] **Step 2: Run `./mvnw test -Dtest=BancoInterCsvParserTest` and confirm parser-class failures.**

- [ ] **Step 3: Parse only rows after the exact five metadata lines and expected header; use `;`, `UTF_8`, `dd/MM/uuuu`, `trim()` and `replace(',', '.')` before `BigDecimal`.**

- [ ] **Step 4: Rerun the parser test and the complete backend suite.**

- [ ] **Step 5: Commit.**

Run: `git add backend; git commit -m "feat(import): parse banco inter csv statements"`.

### Task 4: Persist imports, transactions and categorization rules

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__transactions_and_rules.sql`
- Create: `backend/src/main/java/com/sergio/financial/transaction/*`
- Create: `backend/src/main/java/com/sergio/financial/rule/*`
- Create: `backend/src/main/java/com/sergio/financial/importer/ImportController.java`
- Test: `backend/src/test/java/com/sergio/financial/importer/ImportControllerIT.java`
- Test: `backend/src/test/java/com/sergio/financial/transaction/TransactionControllerIT.java`

**Interfaces:**
- `POST /api/v1/imports` is multipart field `file` and returns `{ "importedCount", "duplicateCount", "transactions": [...] }`.
- `GET /api/v1/transactions?month=YYYY-MM` returns the caller's transactions.
- `PATCH /api/v1/transactions/{id}/category` accepts `{ "categoryId", "learn": true }`; `POST /api/v1/transactions` accepts manual `{ "date", "description", "amount", "categoryId", "type" }`.

- [ ] **Step 1: Write failing integration tests that import a fixture, retain a duplicate candidate with `needsReview=true`, reject a foreign user's transaction update, apply a learned rule before system keyword rules, and create a manual transaction.**

- [ ] **Step 2: Run the two focused integration test classes and observe missing persistence/API failures.**

- [ ] **Step 3: Add `transactions` and `category_rules` migrations, repositories scoped by user id, normalization via lowercase Unicode-safe trimmed text, and system rules for food, transport, market and investments.**

- [ ] **Step 4: Rerun focused integration tests and `./mvnw test`.**

- [ ] **Step 5: Commit.**

Run: `git add backend; git commit -m "feat(transactions): import and categorize spending"`.

### Task 5: Add budgets and dashboard aggregates

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__budgets.sql`
- Create: `backend/src/main/java/com/sergio/financial/budget/*`
- Create: `backend/src/main/java/com/sergio/financial/dashboard/*`
- Test: `backend/src/test/java/com/sergio/financial/dashboard/DashboardControllerIT.java`

**Interfaces:**
- `PUT /api/v1/budgets/{categoryId}?month=YYYY-MM` accepts `{ "limit": 500.00 }`; `GET /api/v1/budgets?month=YYYY-MM` returns spent, limit and `exceeded`.
- `GET /api/v1/dashboard?month=YYYY-MM` returns `{ "byCategory":[{ "categoryId", "categoryName", "spent" }], "monthlyEvolution":[{ "month", "income", "expense" }], "budgets":[...] }`.

- [ ] **Step 1: Write failing tests proving budget upsert is user-scoped, only expense values count toward spent totals, and a limit below spent returns `exceeded=true`.**

- [ ] **Step 2: Run `./mvnw test -Dtest=DashboardControllerIT` and observe endpoint failures.**

- [ ] **Step 3: Add budget migration, aggregate queries based on `BigDecimal`, and dashboard/budget controllers.**

- [ ] **Step 4: Rerun the focused class and all backend tests.**

- [ ] **Step 5: Commit.**

Run: `git add backend; git commit -m "feat(dashboard): add budgets and spending aggregates"`.

### Task 6: Build the frontend application shell and authentication flow

**Files:**
- Create: `frontend/src/lib/api.ts`, `frontend/src/lib/auth.ts`, `frontend/src/types/api.ts`
- Create: `frontend/src/pages/LoginPage.tsx`, `frontend/src/pages/RegisterPage.tsx`, `frontend/src/pages/DashboardPage.tsx`
- Create: `frontend/src/components/AppLayout.tsx`, `frontend/src/components/ProtectedRoute.tsx`
- Test: `frontend/src/pages/LoginPage.test.tsx`, `frontend/src/components/ProtectedRoute.test.tsx`

**Interfaces:**
- `api.ts` reads `VITE_API_URL`, attaches a persisted bearer token and converts non-2xx JSON problem responses into `{ status, message }`.
- Login saves the returned token/user and redirects to `/dashboard`; unauthenticated `/dashboard` redirects to `/login`.

- [ ] **Step 1: Write failing React tests for the unauthenticated redirect and rendering an API validation message after a failed login.**

- [ ] **Step 2: Run `npm test -- --run LoginPage ProtectedRoute` and observe missing component failures.**

- [ ] **Step 3: Add React Router, auth storage/context, typed API client, Tailwind base styles and minimal accessible forms.**

- [ ] **Step 4: Rerun focused tests, then `npm test -- --run` and `npm run build`.**

- [ ] **Step 5: Commit.**

Run: `git add frontend; git commit -m "feat(frontend): add authentication flow"`.

### Task 7: Implement frontend import and transaction review

**Files:**
- Create: `frontend/src/pages/TransactionsPage.tsx`
- Create: `frontend/src/components/StatementUpload.tsx`, `frontend/src/components/TransactionTable.tsx`, `frontend/src/components/CategoryEditor.tsx`
- Test: `frontend/src/components/StatementUpload.test.tsx`, `frontend/src/components/TransactionTable.test.tsx`

**Interfaces:**
- The upload component sends multipart `file` to `/imports`, displays imported/duplicate counts and a friendly server error.
- The transaction table marks duplicate candidates and invokes `PATCH /transactions/{id}/category` with `learn: true` when requested.

- [ ] **Step 1: Write failing tests for rejected file feedback, duplicate-review badge and category edit payload.**

- [ ] **Step 2: Run the focused Vitest files and observe failures.**

- [ ] **Step 3: Add the upload/review route and accessible transaction/category controls using the typed API client.**

- [ ] **Step 4: Rerun focused tests, entire frontend suite and production build.**

- [ ] **Step 5: Commit.**

Run: `git add frontend; git commit -m "feat(frontend): review imported transactions"`.

### Task 8: Implement dashboard charts, budget controls and manual transactions

**Files:**
- Create: `frontend/src/components/CategoryPieChart.tsx`, `frontend/src/components/MonthlyChart.tsx`, `frontend/src/components/BudgetList.tsx`, `frontend/src/components/ManualTransactionForm.tsx`
- Modify: `frontend/src/pages/DashboardPage.tsx`
- Test: `frontend/src/components/BudgetList.test.tsx`, `frontend/src/components/ManualTransactionForm.test.tsx`

**Interfaces:**
- Dashboard consumes `GET /dashboard?month=YYYY-MM`; charts render category spending and monthly evolution.
- Budget list highlights `exceeded` rows and updates limits; manual form posts exactly the Task 4 transaction payload.

- [ ] **Step 1: Write failing tests for an exceeded-budget alert and the manual form's submitted numeric amount/category.**

- [ ] **Step 2: Run the focused tests and observe missing UI failures.**

- [ ] **Step 3: Add responsive Tailwind layout, Recharts visualizations, month selector, budget controls and manual transaction form.**

- [ ] **Step 4: Rerun focused tests, complete frontend tests and build.**

- [ ] **Step 5: Commit.**

Run: `git add frontend; git commit -m "feat(frontend): visualize budgets and spending"`.

### Task 9: Document, integrate and validate the MVP

**Files:**
- Create: `docs/qa-test-plan.md`, `docs/api.md`, `docs/fixtures/README.md`
- Modify: `README.md`, `Estado da Implementação` note, `Decisões de Arquitetura e Contrato de API` note

**Interfaces:**
- README documents fixture-only setup, Docker/API/client commands and no-real-data policy.
- QA plan includes signup, login, valid/invalid import, empty description, negative value, duplicate review, learned rule, manual transaction, charts, exceeded budget and mobile viewport.

- [ ] **Step 1: Write a failing end-to-end smoke check or reproduce a missing documented command.**

- [ ] **Step 2: Start PostgreSQL, backend and frontend with fixture-only data; execute backend and frontend suites.**

- [ ] **Step 3: Use the Maestri Portal to perform the QA plan in desktop and mobile viewports; log evidence and defects in the shared state note.**

- [ ] **Step 4: Ask Aegis to review the final diff for security, user isolation, `BigDecimal`, validation and PRD scope; resolve Critical/Important findings and rerun covering tests.**

- [ ] **Step 5: Commit documentation/integration-only changes.**

Run: `git add README.md docs; git commit -m "docs: add setup and qa guidance"`.
