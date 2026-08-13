# Dashboard Ledger Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the financial dashboard into a dark ledger application with real routes, persistent filtering, complete management flows, and verified API support.

**Architecture:** Spring Boot remains the API source for filtered aggregates, transaction pages, import preview, categories, rules, and budgets. React renders protected route pages inside a responsive shell and stores the global movement filter in `localStorage`.

**Tech Stack:** Java 21, Spring Boot, JPA, React 19, TypeScript, React Router 7, Tailwind CSS 3, Recharts, Lucide React, Vitest, Testing Library.

## Global Constraints

- Use exact semantic tokens: `#14161B`, `#1C1F26`, `#242832`, `#2E323C`, `#EDEAE3`, `#8B8F98`, `#2FA88A`, `#A6435C`, `#D9A441`, `#4C8BF5`.
- Use Fraunces (display), IBM Plex Sans (UI), IBM Plex Mono (money/table); only Lucide icons; no emoji.
- No gradients, glass, blur, or Tailwind green/red. Positive/negative use only `accent-income`/`accent-expense`.
- Sidebar: 256px expanded, 64px collapsed, 300ms tooltip, active 3px income border, 150ms hover; drawer below 768px.
- Cards use 10px radius, controls 6px, 24px gutter, visible focus and reduced-motion support.
- `both` includes every type; `income` includes `INCOME`; `expense` includes `EXPENSE`; investments appear only in `both`.
- Every task uses a red-green-refactor test cycle and a verified English Conventional Commit.

---

## File Structure

- API: `backend/src/main/java/com/sergio/financial/{transaction,dashboard,importer,category,rule}`.
- Shell/state: `frontend/src/{index.css,App.tsx,lib/movement-filter.tsx,components/AppLayout.tsx,components/Sidebar.tsx}`.
- Pages: `frontend/src/pages/{DashboardPage,TransactionsPage,ImportPage,CategoriesPage,GoalsPage,SettingsPage}.tsx`.
- Dashboard components: `frontend/src/components/{KpiStrip,CategoryBarChart,MonthlyChart,TransactionTable,DashboardSkeleton,ComponentError,LedgerPanel}.tsx`.

### Task 1: Ledger tokens, application shell, named routes, and movement filter

**Files:** Modify `frontend/package.json`, `package-lock.json`, `tailwind.config.ts`, `src/index.css`, `src/App.tsx`, `src/components/AppLayout.tsx`. Create `src/lib/movement-filter.tsx`, `src/components/{Sidebar,MovementFilter}.tsx`, and placeholder route pages. Test `src/App.test.tsx`, `src/lib/movement-filter.test.tsx`, `src/components/Sidebar.test.tsx`.

**Interfaces:** `type MovementFilter = 'both' | 'income' | 'expense'`; `useMovementFilter()` returns `{ filter, setFilter }`. Protected routes are `/dashboard`, `/transacoes`, `/importar`, `/categorias`, `/metas`, `/configuracoes`; `/transactions` redirects to `/transacoes`.

- [ ] **Step 1: Write the failing tests**

```tsx
expect(screen.getByRole('link', { name: 'Transações' })).toHaveAttribute('href', '/transacoes')
await user.click(screen.getByRole('button', { name: 'Receitas' }))
expect(localStorage.getItem('financial-controller.movement-filter')).toBe('income')
```

- [ ] **Step 2: Verify red**

Run: `npm test -- App.test.tsx movement-filter.test.tsx Sidebar.test.tsx`
Expected: fails because the route shell and provider do not exist.

- [ ] **Step 3: Implement the smallest complete behavior**

```tsx
const [filter, setFilterState] = useState<MovementFilter>(() => readStoredFilter())
const setFilter = (next: MovementFilter) => { localStorage.setItem('financial-controller.movement-filter', next); setFilterState(next) }
```

Register all named tokens in CSS and Tailwind, load the three fonts, use Lucide navigation, create skip link and `<main id="main-content">`, then implement sidebar collapse/mobile drawer and segmented control.

- [ ] **Step 4: Verify green**

Run: `npm test -- App.test.tsx movement-filter.test.tsx Sidebar.test.tsx && npm run build`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(frontend): add ledger navigation shell"
```

### Task 2: Filtered dashboard aggregate and paginated transaction API

**Files:** Modify `backend/src/main/java/com/sergio/financial/transaction/{TransactionController,TransactionService,FinancialTransactionRepository}.java` and `dashboard/{DashboardController,DashboardService}.java`. Create `transaction/TransactionPageResponse.java` and `dashboard/DashboardFilter.java`. Test `TransactionControllerIT.java` and `DashboardControllerIT.java`.

**Interfaces:** `GET /transactions?month=YYYY-MM&type=INCOME|EXPENSE&categoryId=&from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=10` returns `{content,page,size,totalElements,totalPages}`. `GET /dashboard?month=YYYY-MM&filter=both|income|expense` returns `{totals,byCategory,monthlyEvolution,budgets}`.

- [ ] **Step 1: Write failing controller tests**

```java
mockMvc.perform(get("/api/v1/transactions?month=2026-08&type=EXPENSE&page=0&size=1").header(AUTHORIZATION, bearer()))
    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
mockMvc.perform(get("/api/v1/dashboard?month=2026-08&filter=income").header(AUTHORIZATION, bearer()))
    .andExpect(jsonPath("$.byCategory[0].categoryName").value("Salário"));
```

- [ ] **Step 2: Verify red**

Run: `./mvnw.cmd -Dtest=TransactionControllerIT,DashboardControllerIT test`
Expected: fails because the page envelope, totals, and selected categories are absent.

- [ ] **Step 3: Implement query and total records**

```java
Page<FinancialTransaction> page = transactions.search(userId, month.atDay(1), month.plusMonths(1).atDay(1), type, categoryId, from, to, pageable);
return TransactionPageResponse.from(page.map(this::response));
```

Aggregate categories descending for the selected type; calculate balance, income, expense, largest expense category, and previous-month comparisons; retain both historical series; enforce current-user ownership.

- [ ] **Step 4: Verify green**

Run: `./mvnw.cmd -Dtest=TransactionControllerIT,DashboardControllerIT test && ./mvnw.cmd test`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add backend
git commit -m "feat(api): filter financial dashboard data"
```

### Task 3: CSV preview and user-managed categories/rules API

**Files:** Modify `importer/ImportController.java`, `category/{Category,CategoryController,CategoryRepository}.java`, and `rule/{CategoryRule,CategoryRuleRepository}.java`. Create `importer/ImportPreviewResponse.java`, `category/{CategoryRequest,CategoryService}.java`, `rule/{CategoryRuleController,CategoryRuleRequest,CategoryRuleResponse,CategoryRuleService}.java`. Test `ImportControllerIT.java`, `CategoryControllerIT.java`, `CategoryRuleControllerIT.java`.

**Interfaces:** `POST /imports/preview` parses but never inserts and returns `{rows,previewCount,duplicateCount}`. Add user-scoped `POST|DELETE /categories` and `GET|POST|DELETE /category-rules`.

- [ ] **Step 1: Write failing API tests**

```java
mockMvc.perform(multipart("/api/v1/imports/preview").file(csv).header(AUTHORIZATION, bearer()))
    .andExpect(status().isOk()).andExpect(jsonPath("$.previewCount").value(2));
```

- [ ] **Step 2: Verify red**

Run: `./mvnw.cmd -Dtest=ImportControllerIT,CategoryControllerIT,CategoryRuleControllerIT test`
Expected: fails because preview and management endpoints are missing.

- [ ] **Step 3: Implement user-scoped operations**

```java
@PostMapping("/preview")
public ImportPreviewResponse preview(@RequestParam("file") MultipartFile file, Authentication authentication) {
    return imports.preview(Long.valueOf(authentication.getName()), file);
}
```

Reuse the Banco Inter parser but never save preview rows. Validate nonblank names/keywords, prohibit system/used-category deletion, and preserve existing error response format.

- [ ] **Step 4: Verify green**

Run: `./mvnw.cmd -Dtest=ImportControllerIT,CategoryControllerIT,CategoryRuleControllerIT test && ./mvnw.cmd test`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add backend
git commit -m "feat(api): add import preview and category rules"
```

### Task 4: Dashboard KPIs, charts, states, and transaction review table

**Files:** Modify `frontend/src/lib/{api.ts}`, `types/api.ts`, `pages/{DashboardPage,TransactionsPage}.tsx`, `components/{TransactionTable,MonthlyChart,CategoryPieChart}.tsx`. Create `components/{KpiStrip,CategoryBarChart,DashboardSkeleton,ComponentError,LedgerPanel}.tsx`. Test `DashboardPage.test.tsx`, `TransactionTable.test.tsx`, `Charts.test.tsx`.

**Interfaces:** `getDashboard(month, filter)` and `getTransactions(query)` consume Task 2 shapes. `TransactionTable` uses `useMovementFilter`, category/date filters, and pages of 10 rows.

- [ ] **Step 1: Write failing UI tests**

```tsx
await user.click(screen.getByRole('button', { name: 'Despesas' }))
expect(await screen.findByText('Total gasto no mês')).toBeInTheDocument()
expect(screen.getByRole('button', { name: 'Página 2' })).toBeInTheDocument()
```

- [ ] **Step 2: Verify red**

Run: `npm test -- DashboardPage.test.tsx TransactionTable.test.tsx Charts.test.tsx`
Expected: fails because KPI/filter behavior, bar chart, skeletons, and numeric pagination are absent.

- [ ] **Step 3: Implement data views**

```tsx
const visibleSeries = filter === 'both' ? ['income', 'expense'] : filter === 'income' ? ['income'] : ['expense']
<BarChart layout="vertical" data={data}><Bar dataKey="spent" fill="var(--accent-income)" /></BarChart>
```

Create exact-shape skeletons, per-panel retry banners, Lucide outlined import empty state, 4 KPI cards, 7/5/12 grid, low-opacity 24px ledger ruling, sticky table header, 44px table rows, Plex Mono values, text+color indicators, and a horizontal table wrapper on mobile.

- [ ] **Step 4: Verify green**

Run: `npm test -- DashboardPage.test.tsx TransactionTable.test.tsx Charts.test.tsx && npm test && npm run build`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(frontend): redesign finance dashboard views"
```

### Task 5: Import, categories, goals, and settings pages

**Files:** Modify `frontend/src/lib/{api.ts}`, `types/api.ts`, `components/{StatementUpload,BudgetList}.tsx`, and all four supporting pages. Create `components/{ImportPreview,CategoryRulesEditor,GoalsList,SettingsPreferences}.tsx`. Test `StatementUpload.test.tsx`, `CategoriesPage.test.tsx`, `GoalsPage.test.tsx`, `SettingsPage.test.tsx`.

**Interfaces:** `previewStatement(file)` precedes `uploadStatement(file)`. Goals use `getBudgets(month)` and `updateBudget(categoryId, month, limit)`.

- [ ] **Step 1: Write failing workflow tests**

```tsx
await user.upload(screen.getByLabelText('Arquivo CSV'), csvFile)
expect(await screen.findByText('Prévia: 2 movimentações')).toBeInTheDocument()
expect(screen.getByRole('button', { name: 'Confirmar importação' })).toBeEnabled()
```

- [ ] **Step 2: Verify red**

Run: `npm test -- StatementUpload.test.tsx CategoriesPage.test.tsx GoalsPage.test.tsx SettingsPage.test.tsx`
Expected: fails because preview/confirm, rules, full goals, and preferences are absent.

- [ ] **Step 3: Implement the management pages**

```tsx
const percent = budget.limit === 0 ? 0 : Math.min(100, Math.round((budget.spent / budget.limit) * 100))
<div aria-label={`${budget.categoryName}: ${percent}% da meta`} className="goal-progress" style={{ width: `${percent}%` }} />
```

Keep CSV import two-step. Support category and rule create/delete; show three at-risk goals on dashboard and all editable goals at `/metas`; show account, persisted filter, and fixed dark theme status at `/configuracoes`.

- [ ] **Step 4: Verify green**

Run: `npm test -- StatementUpload.test.tsx CategoriesPage.test.tsx GoalsPage.test.tsx SettingsPage.test.tsx && npm test && npm run build`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(frontend): add financial management pages"
```

### Task 6: Acceptance testing and documentation

**Files:** Modify `README.md` and `frontend/src/App.test.tsx`. Test every backend and frontend test plus browser acceptance checks.

- [ ] **Step 1: Write a failing protected-route coverage test**

```tsx
for (const route of ['/dashboard', '/transacoes', '/importar', '/categorias', '/metas', '/configuracoes']) {
  window.history.pushState({}, '', route)
  expect(await screen.findByRole('main')).toBeInTheDocument()
}
```

- [ ] **Step 2: Verify route coverage**

Run: `npm test -- App.test.tsx`
Expected: every named route renders its actual page.

- [ ] **Step 3: Update docs and browser-test at 1440px and 375px**

Document all routes, ledger tokens/fonts, filter semantics, preview→confirm import, rules, goals, and local commands. Check drawer, filter persistence, preview, category rules, numeric pagination, keyboard focus, and mobile table overflow.

- [ ] **Step 4: Run fresh full verification**

Run: `cd backend && .\mvnw.cmd test; cd ..\frontend && npm test && npm run build`
Expected: all backend tests, frontend tests, and production build exit 0.

- [ ] **Step 5: Commit**

```bash
git add README.md frontend/src/App.test.tsx
git commit -m "docs: document ledger dashboard experience"
```

## Self-Review

- Spec coverage: Tasks 1 and 4 implement every visual/navigation/dashboard requirement; Tasks 2 and 3 supply missing server capabilities; Task 5 implements each remaining real page; Task 6 verifies and documents the full experience.
- Placeholder scan: no TBD, deferred implementation, or unspecified file is present.
- Type consistency: frontend `MovementFilter` maps to dashboard filters and only income/expense maps to `TransactionType` list filters.
