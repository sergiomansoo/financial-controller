# Dashboard Quality Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the local financial dashboard runnable from documented commands and conform its authentication, dashboard, transaction and CORS behavior to the approved ledger redesign.

**Architecture:** Keep the existing React Router, Spring Boot API and PostgreSQL contract. Separate unauthenticated pages from the protected ledger shell, use token-based CSS styles, and keep API filtering as the source of truth for dashboard and transaction data.

**Tech Stack:** React 19, TypeScript, Vite, Vitest, React Testing Library, Recharts, Spring Boot, JUnit/MockMvc, PostgreSQL.

## Global Constraints

- Use Portuguese UI copy for the user-facing financial application.
- Keep dark tokens `#14161B`, `#1C1F26`, `#242832`, `#2E323C`, `#EDEAE3`, `#8B8F98`, `#2FA88A`, `#A6435C`, `#D9A441` and `#4C8BF5`.
- Use no gradients or glass effects; inputs and controls have 6px radius, cards have 10px radius.
- Write a failing test before each production behavior change, then run frontend and backend suites and production build.
- Commit cohesive work with English Conventional Commit messages and push only after fresh verification.

---

### Task 1: Authentication shell and readable controls

**Files:**
- Modify: `frontend/src/App.tsx`, `frontend/src/pages/LoginPage.tsx`, `frontend/src/pages/RegisterPage.tsx`, `frontend/src/index.css`
- Test: `frontend/src/pages/RegisterPage.test.tsx`, `frontend/src/pages/LoginPage.test.tsx`, `frontend/src/App.test.tsx`

- [ ] Write tests asserting register/login input tokens have a dark background and readable text, and that unauthenticated routes do not render the sidebar or movement filter.
- [ ] Run `npm test -- RegisterPage.test.tsx LoginPage.test.tsx App.test.tsx` and confirm the tests fail for missing ledger auth styling/shell separation.
- [ ] Introduce an `AuthLayout`, apply `ledger-auth-*` CSS classes to forms and controls, and render `AppLayout` only around protected routes.
- [ ] Re-run the focused tests and commit `fix(frontend): improve authentication usability`.

### Task 2: Dashboard visualization and states

**Files:**
- Modify: `frontend/src/pages/DashboardPage.tsx`, `frontend/src/index.css`
- Create: `frontend/src/components/LedgerCharts.tsx`
- Test: `frontend/src/pages/DashboardPage.test.tsx`, `frontend/src/components/LedgerCharts.test.tsx`

- [ ] Write failing tests for conditional income/expense KPI visibility, import CTA in empty state, Recharts category/evolution chart labels, and localized component errors.
- [ ] Run `npm test -- DashboardPage.test.tsx LedgerCharts.test.tsx` and confirm failures.
- [ ] Replace manual chart/table presentation with Recharts horizontal bars and six-month evolution using named ledger colors; put Balance first, apply conditional KPIs, component skeletons, local error banners, ledger rules and side-by-side risk list.
- [ ] Re-run focused tests and commit `feat(frontend): complete ledger dashboard visualization`.

### Task 3: Transactions, deletion and API CORS behavior

**Files:**
- Modify: `frontend/src/pages/TransactionsPage.tsx`, `frontend/src/index.css`, `backend/src/main/java/com/sergio/financial/config/SecurityConfig.java`
- Test: `frontend/src/pages/TransactionsPage.test.tsx`, `backend/src/test/java/com/sergio/financial/config/SecurityConfigIT.java`

- [ ] Write failing tests for category badge/actions in transactions and DELETE preflight accepting the configured frontend origin.
- [ ] Run focused frontend and Maven tests and confirm expected failures.
- [ ] Add transaction action presentation, category badges, and `DELETE` to allowed CORS methods without widening origins.
- [ ] Re-run focused tests and commit `fix: complete browser transaction and deletion flows`.

### Task 4: Reproducible local runbook and final validation

**Files:**
- Modify: `README.md`
- Test: manual PowerShell commands plus `frontend` build/test and `backend` Maven test

- [ ] Document exact PowerShell commands for a local PostgreSQL running with `postgres`/`postgres`, `APP_CORS_ALLOWED_ORIGIN`, API port 8080 and Vite port 5173.
- [ ] Start both apps with those exact commands and create/login a test account through the browser portal.
- [ ] Run `npm test`, `npm run build`, Maven `test`, and `git diff --check`.
- [ ] Commit `docs: document reproducible local development` and push the verified branch.
