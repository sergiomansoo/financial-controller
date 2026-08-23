# AI Financial Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a protected, Portuguese-language AI assistant page that analyzes the signed-in user's Financial Controller dashboard, budgets, and transactions through Groq local function calling with `openai/gpt-oss-20b`.

**Architecture:** The React client sends the user's question, selected month, and a short in-memory chat history to one protected API endpoint. The Spring Boot API owns the Groq key and executes only a fixed allow-list of local tools backed by existing services; the LLM never receives database credentials, JWTs, or direct database access. The API performs the Groq tool-call loop, returns one final assistant message, and does not persist conversations in the MVP.

**Tech Stack:** Java 21 `java.net.http.HttpClient`, Spring Boot 3.5, Jackson, existing Spring Security/JPA services, Groq Chat Completions API, React 19, TypeScript, React Router 7, Vitest, and React Testing Library.

## Global Constraints

- Use the Groq model ID exactly `openai/gpt-oss-20b`; configure it through `GROQ_MODEL` with that value as the default.
- The only secret is `GROQ_API_KEY`; it must exist only as an environment variable locally/Render and must never be returned, logged, committed, exposed as `VITE_*`, or placed in `.env.example` with a value.
- Keep every assistant route protected by the existing JWT configuration and derive `userId` exclusively from `Authentication`; never accept a user id from the request body.
- Execute only the four explicitly registered local tools below. Do not give the model HTTP, SQL, filesystem, web-search, transaction-creation, or budget-update capabilities.
- For MVP, the assistant is read-only: it may analyze data and suggest actions, but it must not create, edit, import, categorize, or delete financial records.
- Financial context sent to Groq is limited to the authenticated user's requested month and at most 50 transactions. Do not log prompts, responses, tool arguments, or financial data.
- Assistant replies must be in Brazilian Portuguese, identify when data is insufficient, avoid inventing values, and state that they are informational rather than financial advice.
- Retain at most the latest 10 user/assistant text messages in browser memory. Do not write conversation history to local storage or the database in this MVP.
- Preserve the existing API error envelope `{ status, code, message, fieldErrors }`. Map missing/invalid Groq configuration to `503 AI_UNAVAILABLE`; Groq timeout/5xx/rate-limit failures to the same user-safe response.
- Implement behavior test-first. Run focused tests before the full relevant suite, then commit each coherent task using Conventional Commits in English.

---

## Files and Responsibilities

| Path | Change | Responsibility |
| --- | --- | --- |
| `backend/src/main/resources/application.yml` | Modify | Binds Groq URL, API key, model, timeout, and transaction context limit. |
| `backend/src/main/java/com/sergio/financial/config/GroqProperties.java` | Create | Validated configuration record for the external AI client. |
| `backend/src/main/java/com/sergio/financial/assistant/AssistantController.java` | Create | Protected `POST /api/v1/assistant/chat` HTTP boundary. |
| `backend/src/main/java/com/sergio/financial/assistant/AssistantChatRequest.java` | Create | Validates question, month, and prior text messages. |
| `backend/src/main/java/com/sergio/financial/assistant/AssistantChatResponse.java` | Create | Stable response `{ message }` returned to the frontend. |
| `backend/src/main/java/com/sergio/financial/assistant/AssistantService.java` | Create | Prompt construction, bounded tool-call loop, and reply validation. |
| `backend/src/main/java/com/sergio/financial/assistant/FinancialContextTools.java` | Create | Allow-listed functions calling existing dashboard/budget/transaction services. |
| `backend/src/main/java/com/sergio/financial/assistant/GroqClient.java` | Create | Groq-compatible HTTP request/response transport and JSON parsing. |
| `backend/src/main/java/com/sergio/financial/error/AiUnavailableException.java` | Create | Controlled external-provider failure. |
| `backend/src/main/java/com/sergio/financial/error/ApiExceptionHandler.java` | Modify | Converts AI failures into the normal 503 response envelope. |
| `backend/src/test/java/com/sergio/financial/assistant/AssistantControllerIT.java` | Create | JWT, request validation, response shape, and unavailable-provider integration coverage. |
| `backend/src/test/java/com/sergio/financial/error/ApiExceptionHandlerTest.java` | Modify | Controlled `AI_UNAVAILABLE` error-envelope coverage. |
| `backend/src/test/java/com/sergio/financial/assistant/AssistantServiceTest.java` | Create | Tool-loop, tool allow-list, prompt and timeout/error behavior unit coverage. |
| `frontend/src/types/api.ts` | Modify | Assistant request/message/response contracts. |
| `frontend/src/lib/api.ts` | Modify | Typed `askAssistant` client function using existing bearer-token behavior. |
| `frontend/src/components/AppLayout.tsx` | Modify | Auth-aware navigation link to the assistant page. |
| `frontend/src/pages/AssistantPage.tsx` | Create | Accessible chat interface, selected month, privacy note, loading/error/retry state. |
| `frontend/src/pages/AssistantPage.test.tsx` | Create | Request, rendering, history, empty question, error and retry coverage. |
| `frontend/src/App.tsx` | Modify | Protected `/assistant` route. |
| `README.md` | Modify | Groq configuration, privacy boundary, limits, and local/Render setup. |

## API Contract

`POST /api/v1/assistant/chat` requires a bearer token.

```json
{
  "message": "Onde estou gastando mais neste mês?",
  "month": "2026-08",
  "history": [
    { "role": "user", "content": "Como foi julho?" },
    { "role": "assistant", "content": "Em julho..." }
  ]
}
```

Validation: `message` is required, trimmed, and has 1–1,000 characters; `month` is an ISO `YearMonth`; `history` contains 0–10 items, where role is `user` or `assistant` and content is 1–2,000 characters. The success response is:

```json
{ "message": "Neste mês, Alimentação é a maior categoria de despesa: R$ 420,00." }
```

Errors retain the application's standard JSON shape. Return `400 VALIDATION_ERROR` for malformed input, `401 UNAUTHORIZED` without a JWT, and `503 AI_UNAVAILABLE` when the provider cannot be used. Do not expose upstream response bodies.

## Local Tool Contract

Register these JSON-schema functions in the Groq request. The tool executor uses the authenticated `userId`, never tool-supplied identity.

| Tool name | Arguments | Source | Result limits |
| --- | --- | --- | --- |
| `get_monthly_dashboard` | `{ "month": "YYYY-MM" }` | `DashboardService.dashboard(userId, month)` | Existing aggregate response. |
| `get_monthly_budgets` | `{ "month": "YYYY-MM" }` | `BudgetService.list(userId, month)` | Existing category budgets. |
| `list_month_transactions` | `{ "month": "YYYY-MM", "limit": 1..50 }` | `TransactionService.list(userId, month)` | Clamp `limit` to 50; return date, description/history, amount, type and category only. |
| `compare_monthly_totals` | `{ "month": "YYYY-MM" }` | `DashboardService.dashboard` for `month` and `month.minusMonths(1)` | Income/expense totals derived from `monthlyEvolution`. |

The assistant loop may call at most three tools and make at most four Groq requests (initial request plus follow-ups). If the model asks for an unknown tool, malformed JSON, or exceeds these limits, terminate safely with `AI_UNAVAILABLE`; never fall back to arbitrary execution.

### Task 1: Configure the Groq boundary and provider-safe errors

**Files:**
- Create: `backend/src/main/java/com/sergio/financial/config/GroqProperties.java`
- Create: `backend/src/main/java/com/sergio/financial/error/AiUnavailableException.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/sergio/financial/error/ApiExceptionHandler.java`
- Test: `backend/src/test/java/com/sergio/financial/error/ApiExceptionHandlerTest.java`

**Interfaces:**
- Produces `GroqProperties(String apiKey, String model, URI baseUrl, Duration timeout, int transactionContextLimit)` with model default `openai/gpt-oss-20b`, URL default `https://api.groq.com/openai/v1`, 20-second timeout, and context limit `50`.
- Produces an exception mapped to `503` with code `AI_UNAVAILABLE` and message `O assistente de IA está indisponível no momento. Tente novamente em instantes.`

- [ ] **Step 1: Write the failing unavailable-provider error-handler test.**

```java
@Test
void mapsAiUnavailableToTheSafeServiceUnavailableEnvelope() {
    ResponseEntity<ErrorResponse> response = handler.handleAiUnavailable(new AiUnavailableException());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isEqualTo(new ErrorResponse(503, "AI_UNAVAILABLE",
        "O assistente de IA está indisponível no momento. Tente novamente em instantes."));
}
```

- [ ] **Step 2: Run the focused test and confirm it fails because no assistant endpoint/error mapping exists.**

Run: `Set-Location backend; .\mvnw.cmd test -Dtest=ApiExceptionHandlerTest`

Expected: FAIL at compilation because `AiUnavailableException` and the error mapping do not exist.

- [ ] **Step 3: Add strongly typed configuration and the safe error mapping.**

```yml
groq:
  api-key: ${GROQ_API_KEY:}
  model: ${GROQ_MODEL:openai/gpt-oss-20b}
  base-url: ${GROQ_BASE_URL:https://api.groq.com/openai/v1}
  timeout: ${GROQ_TIMEOUT:20s}
  transaction-context-limit: ${GROQ_TRANSACTION_CONTEXT_LIMIT:50}
```

```java
@ConfigurationProperties(prefix = "groq")
public record GroqProperties(String apiKey, String model, URI baseUrl,
                             Duration timeout, int transactionContextLimit) { }

public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException() { super("AI provider unavailable"); }
    public AiUnavailableException(Throwable cause) { super("AI provider unavailable", cause); }
}

@ExceptionHandler(AiUnavailableException.class)
ResponseEntity<ErrorResponse> aiUnavailable() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ErrorResponse(503, "AI_UNAVAILABLE",
            "O assistente de IA está indisponível no momento. Tente novamente em instantes."));
}
```

Enable configuration-properties scanning in the application class if it is not already enabled. Reject a blank API key before any HTTP call. Do not include the key in error messages.

- [ ] **Step 4: Rerun the focused test and the backend suite.**

Run: `Set-Location backend; .\mvnw.cmd test -Dtest=ApiExceptionHandlerTest; .\mvnw.cmd test`

Expected: PASS.

- [ ] **Step 5: Commit the configuration/error boundary.**

Run: `git add backend; git commit -m "feat(ai): configure groq provider boundary"`

### Task 2: Implement bounded Groq local tool calling

**Files:**
- Create: `backend/src/main/java/com/sergio/financial/assistant/GroqClient.java`
- Create: `backend/src/main/java/com/sergio/financial/assistant/FinancialContextTools.java`
- Create: `backend/src/main/java/com/sergio/financial/assistant/AssistantService.java`
- Test: `backend/src/test/java/com/sergio/financial/assistant/AssistantServiceTest.java`

**Interfaces:**
- `String AssistantService.answer(Long userId, AssistantChatRequest request)` returns only the final assistant content or throws `AiUnavailableException`.
- `GroqClient.complete(List<ObjectNode> messages, ArrayNode tools)` sends `POST {baseUrl}/chat/completions`, authorization header, and model from `GroqProperties`.
- `FinancialContextTools.execute(Long userId, String name, JsonNode arguments)` returns a serializable JSON result only for the four tools in the tool-contract table.

- [ ] **Step 1: Write failing unit tests for tool execution and loop bounds.**

```java
@Test
void executesOnlyAnAllowedToolForTheAuthenticatedUser() {
    when(groqClient.complete(any(), any())).thenReturn(toolCall("get_monthly_dashboard", "{\"month\":\"2026-08\"}"))
        .thenReturn(finalAnswer("Alimentação foi sua maior despesa."));

    assertThat(service.answer(42L, request("Qual foi a maior categoria?", "2026-08")))
        .isEqualTo("Alimentação foi sua maior despesa.");
    verify(dashboardService).dashboard(42L, YearMonth.of(2026, 8));
}

@Test
void rejectsAnUnknownOrFourthToolCallWithoutExecutingIt() {
    when(groqClient.complete(any(), any())).thenReturn(toolCall("drop_database", "{}"));

    assertThatThrownBy(() -> service.answer(42L, request("Apague tudo", "2026-08")))
        .isInstanceOf(AiUnavailableException.class);
    verifyNoInteractions(transactionService, budgetService, dashboardService);
}
```

- [ ] **Step 2: Run the focused test and confirm it fails because the assistant service does not exist.**

Run: `Set-Location backend; .\mvnw.cmd test -Dtest=AssistantServiceTest`

Expected: FAIL at compilation.

- [ ] **Step 3: Implement the minimal Groq client, exact schemas, and read-only executor.**

Use Java's built-in HTTP client; no new HTTP or AI SDK dependency is required:

```java
HttpRequest request = HttpRequest.newBuilder(properties.baseUrl().resolve("chat/completions"))
    .timeout(properties.timeout())
    .header("Authorization", "Bearer " + properties.apiKey())
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
    .build();
```

Build the request with `model` set to `properties.model()`, system instructions, prior user/assistant messages, `tools`, `tool_choice: "auto"`, `parallel_tool_calls: false`, `temperature: 0.2`, and `max_completion_tokens: 700`. The system message must say:

```text
Você é o assistente do Financial Controller. Responda somente em português do Brasil.
Use valores e fatos somente quando recebidos nas ferramentas. Não invente dados.
Você só pode analisar; nunca afirma ter alterado transações, categorias ou orçamentos.
Quando não houver dados suficientes, explique qual dado está faltando.
Suas respostas são informativas e não constituem aconselhamento financeiro profissional.
```

For each returned tool call, append its assistant tool-call message and a `role: "tool"` JSON result with its `tool_call_id`, then call Groq again. Convert `HttpTimeoutException`, `IOException`, non-2xx statuses, blank response content, malformed tool arguments, unknown tools, and loop-limit overflow into `AiUnavailableException`. Never include the upstream response body in the exception. Serialize monetary values as decimal JSON values; do not convert them to `double`.

- [ ] **Step 4: Rerun unit tests and the backend suite.**

Run: `Set-Location backend; .\mvnw.cmd test -Dtest=AssistantServiceTest; .\mvnw.cmd test`

Expected: PASS, including existing tests.

- [ ] **Step 5: Commit the read-only tool loop.**

Run: `git add backend; git commit -m "feat(ai): add bounded financial tool calling"`

### Task 3: Expose and validate the protected assistant API

**Files:**
- Create: `backend/src/main/java/com/sergio/financial/assistant/AssistantController.java`
- Create: `backend/src/main/java/com/sergio/financial/assistant/AssistantChatRequest.java`
- Create: `backend/src/main/java/com/sergio/financial/assistant/AssistantChatResponse.java`
- Modify: `backend/src/test/java/com/sergio/financial/assistant/AssistantControllerIT.java`

**Interfaces:**
- Produces `POST /api/v1/assistant/chat` returning `AssistantChatResponse(String message)`.
- `AssistantChatRequest(String message, YearMonth month, List<AssistantHistoryMessage> history)` validates the request contract above.

- [ ] **Step 1: Add failing HTTP tests for authentication, validation, and the returned shape.**

```java
mockMvc.perform(post("/api/v1/assistant/chat")
        .contentType(APPLICATION_JSON)
        .content("{\"message\":\"teste\",\"month\":\"2026-08\",\"history\":[]}"))
    .andExpect(status().isUnauthorized());

mockMvc.perform(post("/api/v1/assistant/chat").header("Authorization", bearerFor(user))
        .contentType(APPLICATION_JSON)
        .content("{\"message\":\" \",\"month\":\"08/2026\",\"history\":[]}"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
```

- [ ] **Step 2: Run the integration class and confirm endpoint/request failures.**

Run: `Set-Location backend; .\mvnw.cmd test -Dtest=AssistantControllerIT`

Expected: FAIL until the endpoint and DTOs exist.

- [ ] **Step 3: Implement DTO validation and controller delegation.**

```java
@RestController
@RequestMapping("/api/v1/assistant")
class AssistantController {
    private final AssistantService assistant;

    @PostMapping("/chat")
    AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request,
                               Authentication authentication) {
        return new AssistantChatResponse(assistant.answer(Long.valueOf(authentication.getName()), request));
    }
}
```

Use `@NotBlank`, `@Size(max = 1000)`, `@NotNull`, `@Valid`, and `@Size(max = 10)` annotations. Whitelist only `user` and `assistant` history roles in the DTO constructor. Do not alter `SecurityConfig`: its existing authenticated catch-all already protects this route.

- [ ] **Step 4: Run focused integration and all backend tests.**

Run: `Set-Location backend; .\mvnw.cmd test -Dtest=AssistantControllerIT; .\mvnw.cmd test`

Expected: PASS.

- [ ] **Step 5: Commit the public assistant contract.**

Run: `git add backend; git commit -m "feat(api): expose protected assistant chat"`

### Task 4: Add the assistant page and typed frontend integration

**Files:**
- Modify: `frontend/src/types/api.ts`
- Modify: `frontend/src/lib/api.ts`
- Modify: `frontend/src/components/AppLayout.tsx`
- Create: `frontend/src/pages/AssistantPage.tsx`
- Create: `frontend/src/pages/AssistantPage.test.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- `askAssistant(input: AssistantChatRequest): Promise<AssistantChatResponse>` calls `POST /assistant/chat` using the existing `request` helper and bearer JWT.
- `AssistantPage` owns `month`, latest ten in-memory messages, draft, submitting, and user-safe API-error state.
- `/assistant` is wrapped in `ProtectedRoute` and is reachable from a visible `Assistente IA` navigation link.

- [ ] **Step 1: Write failing UI tests for a successful question and a provider error.**

```tsx
it('sends the selected month and renders the assistant answer', async () => {
  vi.mocked(askAssistant).mockResolvedValue({ message: 'Transporte representa R$ 180,00.' })
  render(<AssistantPage />)

  await userEvent.type(screen.getByLabelText(/pergunta/i), 'Qual foi a maior despesa?')
  await userEvent.click(screen.getByRole('button', { name: /enviar/i }))

  expect(askAssistant).toHaveBeenCalledWith(expect.objectContaining({
    message: 'Qual foi a maior despesa?', month: expect.stringMatching(/^\d{4}-\d{2}$/), history: [],
  }))
  expect(await screen.findByText('Transporte representa R$ 180,00.')).toBeInTheDocument()
})

it('keeps the question visible and offers retry after an unavailable-provider error', async () => {
  vi.mocked(askAssistant).mockRejectedValue(new ApiError(503, 'AI_UNAVAILABLE', 'O assistente de IA está indisponível no momento. Tente novamente em instantes.'))
  render(<AssistantPage />)
  // submit a question, then assert role=alert, draft/history preservation, and a retry button
})
```

- [ ] **Step 2: Run the focused test and confirm it fails because the assistant page/client types do not exist.**

Run: `Set-Location frontend; npm test -- --run AssistantPage`

Expected: FAIL at module resolution.

- [ ] **Step 3: Add contracts, API call, route, navigation, and accessible UI.**

```ts
export type AssistantRole = 'user' | 'assistant'
export interface AssistantHistoryMessage { role: AssistantRole; content: string }
export interface AssistantChatRequest { message: string; month: string; history: AssistantHistoryMessage[] }
export interface AssistantChatResponse { message: string }

export function askAssistant(input: AssistantChatRequest) {
  return request<AssistantChatResponse>('/assistant/chat', {
    method: 'POST', body: JSON.stringify(input),
  })
}
```

Render a dedicated page headed `Assistente financeiro`. It must include a month input labeled `Mês de análise`; a chat transcript with `aria-live="polite"`; a textarea labeled `Pergunta`; an `Enviar` button disabled for blank/submitting text; a visible sending status; a Portuguese `role="alert"` on failure; and `Tentar novamente`, which repeats the last unsent request. Add this exact privacy text near the form:

```text
Ao enviar uma pergunta, os dados financeiros necessários para a análise são processados pelo provedor de IA Groq. Não envie senhas, chaves ou dados que não estejam no seu controle financeiro.
```

Keep only ten completed messages using `nextMessages.slice(-10)`. Do not use markdown HTML injection; render assistant text as plain React text. The layout navigation must not render on login/register pages, following the existing auth-route behavior or adding a small location check.

- [ ] **Step 4: Run focused frontend tests, entire frontend suite, and production build.**

Run: `Set-Location frontend; npm test -- --run AssistantPage; npm test; npm run build`

Expected: PASS.

- [ ] **Step 5: Commit the assistant user experience.**

Run: `git add frontend; git commit -m "feat(frontend): add financial ai assistant"`

### Task 5: Document configuration and perform end-to-end verification

**Files:**
- Modify: `README.md`
- Modify: `docs/qa-test-plan.md`

**Interfaces:**
- README documents `GROQ_API_KEY` required for assistant use, optional `GROQ_MODEL=openai/gpt-oss-20b`, deployment configuration in Render, and the data/privacy boundary.
- QA plan includes an authenticated assistant scenario using synthetic data and verifies no assistant action can mutate financial records.

- [ ] **Step 1: Add a failing documentation check by locating missing Groq setup instructions.**

Run: `Select-String -Path README.md -Pattern 'GROQ_API_KEY'`

Expected: no match before this task.

- [ ] **Step 2: Add exact local and Render setup instructions.**

```powershell
# backend local only; do not commit this value
$env:GROQ_API_KEY='cole-a-chave-gerada-no-console-groq'
$env:GROQ_MODEL='openai/gpt-oss-20b'
.\mvnw.cmd spring-boot:run
```

Document that Render must hold `GROQ_API_KEY` in the backend service environment, never in the static frontend environment; free-tier rate limits can cause a retryable `503`; and the assistant sends bounded authenticated financial context to Groq only to answer a user question. Add QA cases for: no key (`503`); no JWT (`401`); blank question (`400`); highest spending category; budget-overrun analysis; provider error and retry; protected navigation; response in Portuguese; and evidence that no new transaction/budget is created after a chat request.

- [ ] **Step 3: Run all automated verification.**

Run: `Set-Location backend; .\mvnw.cmd test; Set-Location ..\frontend; npm test; npm run build`

Expected: all commands exit `0`.

- [ ] **Step 4: Run manual browser QA with a synthetic account.**

1. Start PostgreSQL, backend with a real local `GROQ_API_KEY`, and frontend.
2. Register a fixture-only account, import or create synthetic transactions, and set an August budget.
3. Open `/assistant`, ask `Em qual categoria gastei mais em agosto?`, and verify a Portuguese response grounded in the dashboard.
4. Ask `Altere meu orçamento de alimentação para R$ 1.000`, then confirm the reply does not claim an update and the budget remains unchanged after dashboard refresh.
5. Stop/revoke the local key temporarily, submit again, and verify the safe availability message with no secrets visible in browser/network UI.

- [ ] **Step 5: Commit documentation and QA evidence-free changes.**

Run: `git add README.md docs; git commit -m "docs: document financial ai assistant setup"`

## Self-Review

- **Spec coverage:** Tasks 1–3 cover configuration, the exact model, secure Groq integration, local function calling, JWT scoping, error behavior, and no database migration. Task 4 covers the dedicated protected frontend page, in-memory chat history, UX, and typed API integration. Task 5 covers Render/local configuration, privacy disclosure, automated checks, and manual verification.
- **No placeholders:** Every external value, endpoint, tool name, limit, response/error behavior, test command, and UI copy required for MVP is specified. The plan intentionally excludes transaction mutation, persistent chat storage, and autonomous actions.
- **Type consistency:** The frontend sends `AssistantChatRequest` to `POST /assistant/chat`; backend accepts `AssistantChatRequest` and returns `AssistantChatResponse`; both use `{ message, month, history }` and `{ message }`. Local tools receive `userId` only from JWT-derived authentication.
