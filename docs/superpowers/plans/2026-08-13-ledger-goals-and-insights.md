# Ledger Goals and Insights Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct expense aggregation and deliver editable transaction categories, dashboard commitment metrics, spending budgets and savings goals.

**Architecture:** Preserve signed transaction records and normalize absolute expense amounts only in reporting aggregates. Use the existing transaction category patch endpoint; add user-scoped savings-goal persistence and expose the metrics through dashboard and goal routes.

**Tech Stack:** Spring Boot, JPA/Flyway, React, TypeScript, Vitest, JUnit/MockMvc.

## Global Constraints

- TDD RED/GREEN for each behavior; commits are English Conventional Commits.
- Expense amounts remain signed in transaction rows and display positive in spending/budget aggregates.
- All new data is user-scoped; no default cross-user goals.

### Task 1: Expense aggregates

- [ ] Test negative imported EXPENSE values select the largest category and budget spending by absolute amount.
- [ ] Confirm RED in `DashboardControllerIT`.
- [ ] Aggregate `abs(amount)` only for expenses in dashboard, category and budget queries.
- [ ] Confirm backend suite and commit.

### Task 2: Transaction editing and spending budgets

- [ ] Test category PATCH with `learn=false`, view action navigation/filter and optimistic budget-bar refresh.
- [ ] Implement dropdown editor, useful Ver action, white calendar control and positive budget presentation.
- [ ] Confirm frontend suite/build and commit.

### Task 3: Savings goals and dashboard insights

- [ ] Test user-isolated savings goals, monthly plan/save updates, salary commitment and received-invested percentages.
- [ ] Add Flyway schema, services/controllers and dashboard contracts according to `docs/specs/2026-08-13-ledger-insights-goals-contract.md`.
- [ ] Add goal forms/progress and dashboard metric cards.
- [ ] Confirm full suites/build, portal workflow and commit.
