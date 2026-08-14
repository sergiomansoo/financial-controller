# Dashboard Monolith Rebrand Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesenhar visualmente o dashboard de finanças para o estilo Monolith, preservando dados, rotas e comportamentos existentes.

**Architecture:** O frontend mantém as fontes de dados atuais e encapsula os tokens e o layout do rebranding em estilos escopados ao dashboard. `DashboardPage` compõe os quatro KPIs e painéis; `LedgerCharts` mantém Recharts, mas troca a apresentação por linhas monocromáticas e resumo acessível.

**Tech Stack:** React, TypeScript, CSS, Recharts, Lucide React, Vitest/Testing Library.

## Global Constraints

- Não modificar contratos backend, URLs, autenticação ou o estado global do filtro.
- Usar Inter para interface e JetBrains Mono para dados numéricos.
- Não usar gradientes, glassmorphism, ícones em círculos ou cores decorativas.
- Manter acessibilidade de teclado, `aria-label`, foco e alternativa textual de gráficos.
- Usar TDD e Conventional Commits em inglês.

---

### Task 1: Estrutura e tokens do dashboard

**Files:**
- Create: `frontend/src/pages/DashboardPage.css`
- Modify: `frontend/src/index.css`
- Modify: `frontend/src/pages/DashboardPage.tsx`
- Test: `frontend/src/pages/DashboardPage.test.tsx`

- [ ] **Step 1: Write the failing test**

Adicionar teste que espera `data-testid="monolith-kpi-grid"` com os rótulos `Rendas`, `Despesas`, `Economias` e `Investimentos`, além de `main` com `aria-label="Painel financeiro"`.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- --run src/pages/DashboardPage.test.tsx`
Expected: FAIL porque a estrutura Monolith ainda não existe.

- [ ] **Step 3: Write minimal implementation**

Criar tokens `--monolith-*`, importar Inter/JetBrains Mono e aplicar `DashboardPage.css`. Reestruturar somente markup/classes do dashboard para KPI grid e painéis, preservando carregamento, filtro e chamadas atuais.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- --run src/pages/DashboardPage.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add frontend/src/index.css frontend/src/pages/DashboardPage.tsx frontend/src/pages/DashboardPage.css frontend/src/pages/DashboardPage.test.tsx && git commit -m "feat(dashboard): add monolith dashboard shell"`

### Task 2: Métricas e painéis de dados

**Files:**
- Modify: `frontend/src/pages/DashboardPage.tsx`
- Modify: `frontend/src/components/LedgerCharts.tsx`
- Modify: `frontend/src/lib/api.ts`
- Modify: `frontend/src/types/api.ts`
- Test: `frontend/src/pages/DashboardMetrics.test.tsx`
- Test: `frontend/src/components/LedgerCharts.test.tsx`

- [ ] **Step 1: Write the failing test**

Adicionar testes para o KPI de Economias agregado de metas e KPI de Investimentos obtido pelo total de transações, e para gráfico com séries `Rendas` e `Despesas` identificáveis por texto.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- --run src/pages/DashboardMetrics.test.tsx src/components/LedgerCharts.test.tsx`
Expected: FAIL porque os novos KPIs/painel ainda não são compostos.

- [ ] **Step 3: Write minimal implementation**

Usar apenas clientes HTTP existentes para carregar total de investimentos e metas; somar metas no cliente. Exibir os quatro KPIs e renderizar evolução com linha renda sólida/branca e despesa tracejada/cinza, com resumo textual e sem gradiente.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- --run src/pages/DashboardMetrics.test.tsx src/components/LedgerCharts.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add frontend/src/pages/DashboardPage.tsx frontend/src/components/LedgerCharts.tsx frontend/src/lib/api.ts frontend/src/types/api.ts frontend/src/pages/DashboardMetrics.test.tsx frontend/src/components/LedgerCharts.test.tsx && git commit -m "feat(dashboard): show monolith financial metrics"`

### Task 3: Responsividade, shell visual e regressão

**Files:**
- Modify: `frontend/src/components/AppLayout.tsx`
- Modify: `frontend/src/components/Sidebar.tsx`
- Modify: `frontend/src/pages/DashboardPage.css`
- Test: `frontend/src/components/Sidebar.test.tsx`
- Test: `frontend/src/pages/DashboardPage.test.tsx`

- [ ] **Step 1: Write the failing test**

Adicionar teste que mantém os links das rotas atuais e verifica que o dashboard conserva regiões nomeadas para fluxo, detalhamento e metas.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- --run src/components/Sidebar.test.tsx src/pages/DashboardPage.test.tsx`
Expected: FAIL para as novas regiões/classes responsivas.

- [ ] **Step 3: Write minimal implementation**

Aplicar somente os tokens monocromáticos no shell, preservando handlers/links. Incluir media queries para 12-colunas, coluna única e KPIs 2×2; respeitar reduced motion e foco visível.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- --run src/components/Sidebar.test.tsx src/pages/DashboardPage.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add frontend/src/components/AppLayout.tsx frontend/src/components/Sidebar.tsx frontend/src/pages/DashboardPage.css frontend/src/components/Sidebar.test.tsx frontend/src/pages/DashboardPage.test.tsx && git commit -m "style(dashboard): refine monolith responsive layout"`

### Task 4: Verificação e publicação

**Files:**
- Modify: `docs/specs/2026-08-14-dashboard-monolith-rebrand.md`

- [ ] **Step 1: Run verification**

Run: `npm test && npm run build && git diff --check`
Expected: todas as suítes e build passam, sem whitespace errors.

- [ ] **Step 2: QA visual and functional review**

Validar em 375px, 768px, 1024px e 1440px: contraste, ausência de overflow, quatro KPIs, filtros, loading/erro/vazio, rotas e alternativas textuais.

- [ ] **Step 3: Commit documentation and push**

Run: `git add docs/specs/2026-08-14-dashboard-monolith-rebrand.md docs/superpowers/plans/2026-08-14-dashboard-monolith-rebrand.md && git commit -m "docs: define monolith dashboard rebrand" && git push origin feature/financial-controller-rebrand`
