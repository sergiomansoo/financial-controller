# Assistant Routing and Visual Responses Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the financial assistant answer general questions without tools, return safe structured data after financial tool calls, and render responsive cards in the chat.

**Architecture:** `AssistantService` remains the only orchestrator for Groq function calling. It will return an `AssistantChatResponse` containing natural-language content plus an optional server-built visual payload associated with the last permitted tool. The React chat renders that payload through typed, non-interactive presentational components; unknown payloads fall back to text only.

**Tech Stack:** Java 25, Spring Boot, Jackson, React 19, TypeScript, Vitest, Testing Library, existing CSS and lucide-react.

## Global Constraints

- Keep the current React/CSS architecture; do not add Tailwind, shadcn, Radix, or component-library dependencies.
- Tools remain read-only and the authenticated user identity must remain server-controlled.
- General chat must be able to return a direct model answer with no tool invocation.
- Only the backend may create a visual payload; the model must never supply arbitrary visual JSON.
- A category with a null or zero budget is `no_limit`, never `over_limit`.
- Render cards only for a recognized `visualType`; all other assistant messages remain standard text bubbles.
- Preserve the maximum of three tool calls, four model requests, and fifty transactions in a context.
- Maintain the dark, high-contrast design, visible focus states, no icon-only interactive controls, and mobile-safe widths.

---

### Task 1: Define and test the response contract

**Files:**
- Modify: `backend/src/main/java/com/sergio/financial/assistant/AssistantChatResponse.java`
- Modify: `backend/src/main/java/com/sergio/financial/assistant/AssistantController.java`
- Modify: `backend/src/test/java/com/sergio/financial/assistant/AssistantControllerIT.java`

- [ ] Add nullable `visualType` and `visualData` fields to the API response.
- [ ] Add a controller test that asserts a direct answer returns `visualType: null`.
- [ ] Run the controller test and confirm it fails before implementation.
- [ ] Return the new service response directly from the controller.
- [ ] Re-run the controller test and confirm it passes.

### Task 2: Build visual payloads from tool results

**Files:**
- Modify: `backend/src/main/java/com/sergio/financial/assistant/AssistantService.java`
- Modify: `backend/src/test/java/com/sergio/financial/assistant/AssistantServiceTest.java`

- [ ] Add failing service tests for direct general advice with no tool call and a budget tool call producing `budget_summary` data.
- [ ] Record only the final permitted tool result during orchestration.
- [ ] Map `get_monthly_budgets` to `budget_summary`, `get_monthly_dashboard` to `monthly_summary`, `list_month_transactions` to `transactions_list`, and `compare_monthly_totals` to `monthly_comparison`.
- [ ] Calculate the budget status server-side: `no_limit`, `no_spending`, `within_limit`, or `over_limit`.
- [ ] Strengthen the system instruction: general questions receive direct text without tools; tools are used only for account facts.
- [ ] Run `AssistantServiceTest` and confirm all existing safety tests remain green.

### Task 3: Add typed chat visuals to React

**Files:**
- Modify: `frontend/src/types/api.ts`
- Create: `frontend/src/components/AssistantVisualCard.tsx`
- Create: `frontend/src/components/AssistantVisualCard.test.tsx`
- Modify: `frontend/src/pages/AssistantPage.tsx`
- Modify: `frontend/src/pages/AssistantPage.css`

- [ ] Add discriminated visual payload interfaces matching the backend response.
- [ ] Write failing tests covering a budget card’s no-limit label, progress state, monthly summary, and unknown-type fallback.
- [ ] Render a labelled, responsive visual card beneath the related assistant bubble only for recognized types.
- [ ] Use semantic lists, text labels plus color for status, and format amounts in `pt-BR` / BRL.
- [ ] Limit transaction card output to ten visible entries even if the backend context holds fifty.
- [ ] Run the targeted frontend tests and confirm they pass.

### Task 4: Clarify failures and verify integration

**Files:**
- Modify: `backend/src/main/java/com/sergio/financial/error/ApiExceptionHandler.java`
- Modify: `backend/src/test/java/com/sergio/financial/error/ApiExceptionHandlerTest.java`

- [ ] Add failing tests that preserve provider failures as `AI_UNAVAILABLE` and classify malformed model/tool protocol failures as `AI_RESPONSE_INVALID`.
- [ ] Add a dedicated safe exception for invalid upstream model output; do not expose request bodies, secrets, or stack traces.
- [ ] Run backend assistant/error tests.
- [ ] Run `npm test -- --run` and `npm run build` from `frontend`.
- [ ] Run the relevant Maven tests from `backend`.

## Acceptance Criteria

1. “Oi” and “Dê uma dica para economizar” return a normal assistant message without a tool or visual card.
2. A budget question uses approved account data and returns a `budget_summary` card.
3. Budget status never marks zero/null limits as exceeded.
4. Unknown visual types cannot break the chat.
5. Cards work on mobile and desktop while preserving the existing dark theme.
6. Tool validation and data-access constraints remain intact.
7. Tests and production frontend build pass before commit and push.
