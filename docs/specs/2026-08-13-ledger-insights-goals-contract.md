# Contrato — insights, ações de transação e metas

## Escopo

Esta extensão cobre: maior categoria coerente com o filtro, edição de categoria em uma movimentação, ação de consulta, percentuais de salário e metas mensais de gasto e de poupança para objetivos. Todos os recursos são do usuário autenticado.

## Regras de domínio

- `filter=expense`: **maior categoria** é a categoria de `EXPENSE` de maior soma no mês.
- `filter=income`: **maior categoria** é a categoria de `INCOME` de maior soma no mês. O campo não pode se chamar `largestExpenseCategory` nesse caso.
- `filter=both`: retornar dois destaques independentes, `largestExpenseCategory` e `largestIncomeCategory`; `INVESTMENT` não concorre em nenhum dos dois.
- `salaryReceived` é a soma mensal de transações `INCOME` cuja categoria possui `isSalary=true`. `salaryCommitted` é a soma de `EXPENSE` do mesmo mês. `salaryCommittedPercent = 0` quando `salaryReceived = 0`; caso contrário, `round(salaryCommitted / salaryReceived * 100, 2)`.
- `receivedInvested` é a soma mensal de `INVESTMENT`. `receivedInvestedPercent = 0` quando `incomeReceived = 0`; caso contrário, `round(receivedInvested / incomeReceived * 100, 2)`. `incomeReceived` soma todas as receitas, inclusive salário.
- Editar uma categoria só pode atingir uma transação pertencente ao usuário. `learn=true` cria/atualiza a regra para a descrição normalizada; `learn=false` só altera a transação.
- Ações **Ver** são navegação sem mutação: categoria abre `/transacoes?month=YYYY-MM&categoryId={id}`; meta de poupança abre `/metas/{id}?month=YYYY-MM`.
- Uma meta de gasto é por `(userId, categoryId, month)`. Uma meta de poupança é por `(userId, goalId, month)`. Valores monetários são decimais não negativos, com duas casas.

## Modelo

```text
Category(id, userId?, name, isSystem, isSalary)
FinancialTransaction(id, userId, date, amount, type, categoryId, history, description)
SpendingBudget(id, userId, categoryId, month, limit)
SavingsGoal(id, userId, name, targetAmount, targetDate?, active)
SavingsGoalMonth(id, userId, goalId, month, plannedAmount, savedAmount)
```

Índices/constraints: `SpendingBudget UNIQUE(user_id, category_id, month)`, `SavingsGoalMonth UNIQUE(user_id, goal_id, month)`, e índices `(user_id, date)` em transações e `(user_id, month)` nas duas tabelas mensais.

## Endpoints

### Painel

`GET /api/v1/dashboard?month=YYYY-MM&filter=both|income|expense`

Resposta (campos existentes preservados; novos campos aditivos):

```json
{
  "totals": {
    "income": 10000.00,
    "expense": 4200.00,
    "balance": 5800.00,
    "largestExpenseCategory": { "categoryId": 12, "categoryName": "Moradia", "amount": 1800.00 },
    "largestIncomeCategory": { "categoryId": 3, "categoryName": "Salário", "amount": 9000.00 },
    "salaryReceived": 9000.00,
    "salaryCommitted": 4200.00,
    "salaryCommittedPercent": 46.67,
    "incomeReceived": 10000.00,
    "receivedInvested": 1000.00,
    "receivedInvestedPercent": 10.00
  },
  "byCategory": [],
  "monthlyEvolution": [],
  "budgets": [],
  "savingsGoals": []
}
```

Para compatibilidade de transição, o cliente pode aceitar o legado `largestExpenseCategory` string/`largestExpenseAmount`; a API nova deve fornecer o objeto acima. Em `income`, `largestExpenseCategory` é `null`; em `expense`, `largestIncomeCategory` é `null`.

### Editar categoria de transação

`PATCH /api/v1/transactions/{transactionId}/category`

```json
{ "categoryId": 12, "learn": false }
```

`200 OK`: `TransactionResponse` atualizado.

```json
{ "id": 44, "date": "2026-08-12", "history": "PIX", "description": "Aluguel", "amount": 1800.00, "type": "EXPENSE", "category": { "id": 12, "name": "Moradia" }, "needsReview": false }
```

Erros: `400 VALIDATION_ERROR`, `401/403`, `404 TRANSACTION_NOT_FOUND|CATEGORY_NOT_FOUND`. Se `learn=true` for inviável por regra inválida/conflitante, usar `409 CATEGORY_RULE_CONFLICT` sem desfazer a alteração da transação (ou retornar `422 LEARN_RULE_REJECTED` e não alterar nada; escolher uma semântica única na implementação).

### Consultar transações por categoria (ação Ver)

Reutilizar `GET /api/v1/transactions?month=YYYY-MM&categoryId={id}&page=0&size=10`; resposta paginada existente. Não criar endpoint de ação.

### Metas mensais de gasto

Reutilizar `GET /api/v1/budgets?month=YYYY-MM` e `PUT /api/v1/budgets/{categoryId}?month=YYYY-MM` com `{ "limit": 2000.00 }`.

Resposta de gasto:

```json
{ "categoryId": 12, "categoryName": "Moradia", "spent": 1800.00, "limit": 2000.00, "exceeded": false, "progressPercent": 90.00 }
```

### Objetivos e metas mensais de poupança

`GET /api/v1/savings-goals?month=YYYY-MM`

```json
[{ "id": 7, "name": "Reserva", "targetAmount": 30000.00, "targetDate": "2027-12-31", "month": "2026-08", "plannedAmount": 1500.00, "savedAmount": 1200.00, "progressPercent": 80.00, "overallSavedAmount": 8200.00, "overallProgressPercent": 27.33 }]
```

`POST /api/v1/savings-goals`

```json
{ "name": "Reserva", "targetAmount": 30000.00, "targetDate": "2027-12-31" }
```

`PUT /api/v1/savings-goals/{goalId}/months/{YYYY-MM}`

```json
{ "plannedAmount": 1500.00, "savedAmount": 1200.00 }
```

`200 OK` retorna o item de meta mensal no formato acima. `DELETE /api/v1/savings-goals/{goalId}` retorna `204`; não apaga transações/investimentos.

## Erro comum

```json
{ "status": 400, "code": "VALIDATION_ERROR", "message": "Validation failed.", "fieldErrors": { "plannedAmount": "must be greater than or equal to 0.00" } }
```

## Aceite mínimo

- Testar todos os três filtros: cada destaque só usa o tipo permitido, e investimentos não viram maior receita/despesa.
- Testar percentuais com denominador zero, precisão de duas casas e agregação mensal por usuário.
- Testar alteração de categoria, isolamento entre usuários e os dois valores de `learn`.
- Testar que **Ver** conserva mês/filtros e não produz `POST`, `PUT`, `PATCH` ou `DELETE`.
- Testar unicidade do mês para meta de gasto e poupança, atualização idempotente e validação de valores negativos.
