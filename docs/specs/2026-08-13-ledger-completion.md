# Spec — Conclusão de Metas, Insights e Ações de Transação

## Objetivo

Concluir os fluxos de metas e transações para que o dashboard reflita despesas importadas corretamente, permita reclassificação e apresente tanto limites mensais de gasto quanto objetivos de poupança.

## 1. Integridade de metas de poupança

- A exclusão de uma meta deve apagar primeiro todos os registros mensais vinculados do mesmo usuário e, depois, a meta principal, na mesma transação.
- `DELETE /api/v1/savings-goals/{id}` deve responder `204` quando a meta existir e pertencer ao usuário; metas de outros usuários devem responder `404` e não sofrer alteração.
- Não usar cascade sem teste explícito de ownership.

## 2. Dashboard e agregados financeiros

- Despesas importadas com valor negativo continuam negativas na tabela de transações, mas todo agregado de gasto usa `abs(amount)`.
- A maior despesa é a categoria de maior gasto absoluto, ordenada descendentemente; assim, `Outros` com R$ 100 supera Alimentação com R$ 10.
- Expor no dashboard:
  - `salaryCommittedPercent`: `despesas / receitas * 100`, zero quando não houver receitas;
  - `receivedInvestedPercent`: `investimentos / receitas * 100`, zero quando não houver receitas.
- Percentuais são retornados com duas casas decimais e exibidos no dashboard como porcentagem.

## 3. Metas mensais de gastos

- Os valores de gasto, limite e progresso são exibidos como positivos.
- Salvar um novo limite atualiza imediatamente o valor e a largura da barra, sem recarregar a página.
- O progresso é limitado visualmente a 100%; acima de 100% mantém alerta de estouro.

## 4. Objetivos de poupança

- Criar objetivo com nome, valor-alvo e data-alvo.
- Por objetivo e mês, salvar valor planejado e valor efetivamente poupado.
- Exibir progresso mensal (`poupado / planejado`) e total (`poupado acumulado / alvo`).
- A página `/metas` possui duas seções claramente separadas: **Limites mensais de gastos** e **Objetivos de poupança**.

## 5. Transações

- Cada linha possui ação **Editar categoria**, com seletor de categorias e confirmação usando `PATCH /transactions/{id}/category` com `learn: false` por padrão.
- A ação **Ver** deve abrir um detalhe funcional da transação (data, descrição, valor, tipo e categoria), não ser um botão inerte.
- Após reclassificar, a tabela atualiza a categoria mostrada e os dashboards posteriores refletem a mudança.

## 6. Interface

- O ícone/controle nativo do campo de mês/calendário deve ter aparência clara sobre o tema escuro.
- Manter cores ledger, foco acessível e textos em português.

## Aceitação e testes

- Backend: testes de integração para exclusão de meta com meses, isolamento por usuário, percentuais e maior categoria com despesas negativas.
- Frontend: testes para reclassificação, detalhe Ver, atualização otimista de orçamento, percentuais e objetivos de poupança.
- Final: `mvn test`, `npm test`, `npm run build`, `git diff --check` e fluxo portal de cadastro → importar/criar transação → editar categoria → meta gasto → meta poupança.
