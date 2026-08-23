# Financial Controller

Personal finance, presented like a premium financial instrument.

Financial Controller is a full-stack application for importing bank statements, classifying transactions, planning spending, and turning savings goals into measurable progress. The current `main` release uses the **Monolith Noir** visual system: a monochrome, high-contrast interface built around calm data reading, precise typography, and accessible financial summaries.

## Why this project stands out

- **A real product surface:** authentication, transaction workflows, CSV preview/import, categorisation, budgets, savings goals, dashboards, and settings are implemented as connected user journeys.
- **Data integrity by design:** imports are user-scoped, duplicate candidates are preserved for review, invalid files do not create history entries, and Flyway migrations keep the schema reproducible.
- **Financial clarity:** income, expenses, investments, monthly limits, savings contributions, category breakdowns, and six-month evolution are calculated from the same transaction model.
- **A deliberate visual identity:** Monolith Noir replaces generic dashboard styling with a restrained black/gray palette, Inter for interface hierarchy, JetBrains Mono for numbers, and dense but readable cards inspired by financial terminals.
- **Quality as part of delivery:** frontend behavior is covered with Vitest and Testing Library; backend behavior is covered with JUnit, MockMvc, and integration tests; every implementation is published with a Conventional Commit.

## Product capabilities

### Dashboard

- KPI cards for income, expenses, savings, and investments.
- Monthly balance and six-month evolution charts.
- Expense breakdown by category with percentages and accessible data tables.
- Savings plans and combined monthly spending-limit progress.
- Persistent income/expense/both filtering shared with transactions.

### Transactions and imports

- Paginated transaction table with category, date-range, type, and search filters.
- CSV preview before confirmation.
- Import history showing original filename, row count, and import date/time.
- Duplicate detection with review status instead of silent deletion.
- Manual category changes and optional keyword-learning rules.

### Planning

- Monthly spending limits by category.
- Combined limit progress with positive display values.
- Savings goals with target dates, planned monthly amounts, accumulated contributions, remaining amount, and manual deposits.
- Goal progress cards with responsive actions and accessible focus behavior.

## Architecture

```text
React + TypeScript + Vite
        │  REST / JSON + Bearer JWT
        ▼
Spring Boot + Spring Security + JPA
        │  Flyway migrations
        ▼
PostgreSQL
```

| Layer | Stack | Responsibility |
| --- | --- | --- |
| Frontend | React 19, TypeScript, Vite, Recharts, Tailwind | Routing, auth session, forms, charts, filters, responsive UI |
| Backend | Java 21, Spring Boot 3, Spring Security, JPA | REST API, authorization, business rules, validation |
| Database | PostgreSQL, Flyway | User-scoped persistence and schema evolution |
| Quality | Vitest, Testing Library, JUnit, MockMvc | Component, interaction, controller, and integration coverage |

## Run locally

### Prerequisites

- Java 21+
- Node.js 24+
- PostgreSQL 16+ (or Docker)

### Assistente financeiro (Groq)

O assistente usa permanentemente `openai/gpt-oss-20b`. Configure `GROQ_API_KEY` somente no serviço de backend — localmente ou no Render — e nunca como variável `VITE_*` ou no frontend. Ele analisa apenas o contexto autenticado do mês selecionado, não altera dados financeiros e pode retornar `503` em caso de indisponibilidade ou limite do Groq.

### 1. Start PostgreSQL

```powershell
docker run --name financial-controller-postgres --rm `
  -e POSTGRES_DB=financial_controller `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 postgres:16
```

### 2. Start the API

```powershell
Set-Location backend
$env:DATABASE_URL='jdbc:postgresql://localhost:5432/financial_controller'
$env:DATABASE_USERNAME='postgres'
$env:DATABASE_PASSWORD='postgres'
$env:JWT_SECRET='local-development-secret-with-at-least-32-characters'
$env:APP_CORS_ALLOWED_ORIGINS='http://localhost:5173,http://127.0.0.1:5173'
.\mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:8080/api/v1`.

### 3. Start the frontend

```powershell
Set-Location frontend
npm ci
$env:VITE_API_URL='http://localhost:8080/api/v1'
npm run dev
```

Open `http://localhost:5173`. Keep the hostname consistent with the CORS variable. To use different ports, update both the Vite URL and the API CORS allow-list together.

## Test and build

```powershell
# Backend
Set-Location backend
.\mvnw.cmd test

# Frontend
Set-Location ..\frontend
npm ci
npm test
npm run build
```

The frontend build is production-ready and the test suite includes the dashboard, goals, import history, authentication, navigation, filters, and transaction interactions.

## API highlights

All routes except authentication require `Authorization: Bearer <token>`.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Create an account |
| `POST` | `/api/v1/auth/login` | Authenticate and receive a JWT |
| `GET` | `/api/v1/dashboard?month=YYYY-MM&filter=both` | Dashboard metrics and series |
| `GET` | `/api/v1/transactions` | Paginated, filtered transactions |
| `POST` | `/api/v1/imports` | Import a CSV statement |
| `GET` | `/api/v1/imports` | List the authenticated user's import history |
| `GET/POST/DELETE` | `/api/v1/category-rules` | Manage keyword classification rules |
| `GET` | `/api/v1/budgets?month=YYYY-MM` | Read monthly limits |
| `PUT` | `/api/v1/budgets/{categoryId}?month=YYYY-MM` | Create or update a limit |

## Repository map

```text
backend/src/main/java/       API, domain services, security
backend/src/main/resources/  Flyway migrations and configuration
backend/src/test/            Controller and integration tests
frontend/src/components/     Navigation, charts, forms, shared UI
frontend/src/pages/          Route-level screens
frontend/src/lib/             API client and auth session
frontend/src/types/           Shared API contracts
docs/                         Product specs, QA plans, and fixtures
```

## Security and privacy

- Never commit real statements, credentials, JWT secrets, or personal financial data.
- Passwords are hashed with BCrypt.
- API data is scoped to the authenticated user.
- CORS is explicitly configured rather than opened globally.
- The local MVP stores its session in browser storage; production hardening should evaluate HttpOnly cookies and CSRF protection.

## Contribution workflow

1. Create a focused branch.
2. Add or update tests before changing behavior.
3. Run the relevant focused tests, the full suite, and the production build.
4. Use an English Conventional Commit, for example:

   ```text
   feat(goals): add manual savings contributions
   fix(api): scope import history to authenticated users
   style(dashboard): apply Monolith Noir visual system
   ```

5. Push the branch and open a reviewable pull request.

## License

This repository is currently intended as a portfolio and learning project. Add a project-specific license before distributing it as a library or commercial product.
