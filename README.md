<h1 align="center">Financial Controller</h1>

<p align="center">
  Finanças pessoais apresentadas como um instrumento financeiro premium.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-b07219?logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3-6DB33F?logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black" />
  <img src="https://img.shields.io/badge/TypeScript-blue?logo=typescript&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white" />
</p>

---

## Sobre o projeto

**Financial Controller** é uma aplicação full stack para importar extratos bancários, classificar transações, planejar gastos e transformar metas de economia em progresso mensurável. A versão atual segue o sistema visual **Monolith Noir**: uma interface monocromática de alto contraste, construída para leitura calma de dados, tipografia precisa e resumos financeiros acessíveis.

Diferenciais do projeto:

- **Superfície de produto real**: autenticação, importação de CSV com preview, categorização, orçamentos, metas de economia, dashboards e configurações funcionam como jornadas de usuário conectadas, não como telas isoladas
- **Integridade de dados por design**: importações são vinculadas ao usuário, candidatos a duplicata são preservados para revisão manual (em vez de descartados silenciosamente), arquivos inválidos não geram histórico, e migrations com Flyway garantem um schema reproduzível
- **Clareza financeira**: receitas, despesas, investimentos, limites mensais, contribuições de metas, distribuição por categoria e evolução de 6 meses são calculados a partir do mesmo modelo de transação
- **Qualidade como parte da entrega**: front-end coberto com Vitest e Testing Library, back-end coberto com JUnit, MockMvc e testes de integração; todo commit segue o padrão Conventional Commits

---

## Tecnologias utilizadas

| Camada | Stack |
|---|---|
| **Front-end** | React 19, TypeScript, Vite, Recharts, Tailwind CSS |
| **Back-end** | Java 21, Spring Boot 3, Spring Security, JPA |
| **Banco de dados** | PostgreSQL 16, Flyway |
| **IA** | Assistente financeiro via Groq (`openai/gpt-oss-20b`) |
| **Testes** | Vitest, Testing Library (front-end) · JUnit, MockMvc (back-end) |

---

## Funcionalidades

### Dashboard

- Cards de KPI para receitas, despesas, economia e investimentos
- Gráficos de saldo mensal e evolução dos últimos 6 meses
- Distribuição de despesas por categoria, com percentuais e tabelas acessíveis
- Planos de economia e progresso combinado de limites mensais de gasto
- Filtro persistente por receita/despesa/ambos, compartilhado com a tela de transações

### Transações e importações

- Tabela paginada de transações com filtros por categoria, período, tipo e busca
- Preview do CSV antes da confirmação da importação
- Histórico de importações com nome do arquivo original, quantidade de linhas e data/hora
- Detecção de duplicatas com status de revisão (em vez de exclusão automática)
- Alteração manual de categoria e regras opcionais de classificação por palavra-chave

### Planejamento

- Limites de gasto mensais por categoria
- Progresso combinado de limites, com valores exibidos de forma positiva
- Metas de economia com data-alvo, valor mensal planejado, contribuições acumuladas, valor restante e depósitos manuais
- Cards de progresso de metas com ações responsivas e foco acessível

---

## Acesso para teste

Use as credenciais abaixo para explorar a aplicação sem precisar criar uma conta:

**Email:** `teste.financeiro@exemplo.com`<br>
**Senha:** `TesteFinanceiro`

---

## Arquitetura

```
React + TypeScript + Vite
        │  REST / JSON + Bearer JWT
        ▼
Spring Boot + Spring Security + JPA
        │  Flyway migrations
        ▼
PostgreSQL
```

---

## Endpoints principais

Todas as rotas, exceto as de autenticação, exigem o header `Authorization: Bearer <token>`.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/auth/register` | Cria uma conta |
| POST | `/api/v1/auth/login` | Autentica e retorna um JWT |
| GET | `/api/v1/dashboard?month=YYYY-MM&filter=both` | Métricas e séries do dashboard |
| GET | `/api/v1/transactions` | Transações paginadas e filtradas |
| POST | `/api/v1/imports` | Importa um extrato em CSV |
| GET | `/api/v1/imports` | Lista o histórico de importações do usuário autenticado |
| GET/POST/DELETE | `/api/v1/category-rules` | Gerencia regras de classificação por palavra-chave |
| GET | `/api/v1/budgets?month=YYYY-MM` | Lê os limites mensais |
| PUT | `/api/v1/budgets/{categoryId}?month=YYYY-MM` | Cria ou atualiza um limite |

---

## Assistente financeiro com function calling

O projeto conta com um assistente em português, integrado via **Groq** usando o modelo `openai/gpt-oss-20b`, que responde perguntas sobre a situação financeira do usuário dentro do mês selecionado no dashboard.

Em vez de gerar respostas soltas a partir de texto livre, o assistente usa **function calling**: o modelo decide, com base na pergunta, quais funções internas chamar para buscar dados reais antes de formular a resposta. As funções expostas são todas de **leitura**, restritas ao contexto autenticado do usuário:

- Consulta de métricas do dashboard (receitas, despesas, saldo, investimentos)
- Consulta de orçamento e limites por categoria
- Comparação entre receitas e despesas
- Listagem de transações do período

Restrições de design aplicadas:

- **Sem acesso direto** ao banco de dados, ao token JWT ou a senhas — o assistente só enxerga o que as funções expõem
- **Não realiza escrita**: não cria, edita ou apaga transações, metas ou orçamentos
- **Limite de chamadas por interação**: até 4 chamadas de função e 5 chamadas ao provedor Groq, evitando loops ou uso excessivo de tokens
- Retorna `503` de forma controlada em caso de indisponibilidade ou limite do Groq, sem quebrar o restante da aplicação

A decisão de não usar RAG, embeddings ou pgvector foi deliberada: para o escopo do assistente (responder sobre dados estruturados e já existentes no banco), function calling sobre endpoints de leitura é suficiente e evita custo e complexidade operacional desnecessários.

---

## Processo de desenvolvimento: Spec-Driven Development

O projeto foi construído seguindo **Spec-Driven Development (SDD)**: cada funcionalidade nasce de uma especificação escrita antes da implementação, não do código em si.

Fluxo adotado:

1. **Especificação** — definição do comportamento esperado, regras de negócio e formato de dados antes de escrever código (por exemplo, o PRD que define o formato do CSV de extrato com base no padrão do Banco Inter)
2. **Implementação guiada por testes** — o código é escrito para atender à spec, com testes cobrindo o comportamento descrito
3. **Revisão de diffs** — mudanças são revisadas antes de seguir adiante, comparando o que foi implementado com o que foi especificado
4. **Execução de testes antes de cada commit** — garante que a spec continua sendo satisfeita a cada mudança

Essa abordagem reduziu ambiguidade em features com regras de negócio mais sensíveis, como detecção de duplicidade na importação de extratos e cálculo de progresso de metas de economia, já que o comportamento esperado ficava definido antes de qualquer linha de código.

---

## Estrutura do projeto

```
backend/src/main/java/       # API, serviços de domínio, segurança
backend/src/main/resources/  # Migrations Flyway e configuração
backend/src/test/            # Testes de controller e integração
frontend/src/components/     # Navegação, gráficos, formulários, UI compartilhada
frontend/src/pages/          # Telas por rota
frontend/src/lib/            # Cliente da API e sessão de autenticação
frontend/src/types/          # Contratos de API compartilhados
docs/                        # Specs de produto, planos de QA e fixtures
```

---

## Como rodar o projeto

### Pré-requisitos

- Java 21+
- Node.js 24+
- PostgreSQL 16+ (ou Docker)

### 1. Subir o PostgreSQL

```powershell
docker run --name financial-controller-postgres --rm `
  -e POSTGRES_DB=financial_controller `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 postgres:16
```

### 2. Iniciar a API

```powershell
Set-Location backend
$env:DATABASE_URL='jdbc:postgresql://localhost:5432/financial_controller'
$env:DATABASE_USERNAME='postgres'
$env:DATABASE_PASSWORD='postgres'
$env:JWT_SECRET='local-development-secret-with-at-least-32-characters'
$env:APP_CORS_ALLOWED_ORIGINS='http://localhost:5173,http://127.0.0.1:5173'
.\mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8080/api/v1`.

### 3. Iniciar o front-end

```powershell
Set-Location frontend
npm ci
$env:VITE_API_URL='http://localhost:8080/api/v1'
npm run dev
```

Acesse `http://localhost:5173`. Mantenha o hostname consistente com a variável de CORS — se mudar as portas, atualize a URL do Vite e a allow-list da API juntas.

### Assistente financeiro (Groq)

O assistente usa permanentemente `openai/gpt-oss-20b`. Configure `GROQ_API_KEY` **apenas no serviço de back-end** — localmente ou no Render — e nunca como variável `VITE_*` ou no front-end. Ele analisa apenas o contexto autenticado do mês selecionado, não altera dados financeiros e pode retornar `503` em caso de indisponibilidade ou limite do Groq.

### Testes e build

```powershell
# Back-end
Set-Location backend
.\mvnw.cmd test

# Front-end
Set-Location ..\frontend
npm ci
npm test
npm run build
```

O build do front-end é pronto para produção, e a suíte de testes cobre dashboard, metas, histórico de importação, autenticação, navegação, filtros e interações com transações.

---

## Licença

Este repositório é, por enquanto, um projeto de portfólio e aprendizado. Adicione uma licença específica antes de distribuí-lo como biblioteca ou produto comercial.

---

## Autor

**Sérgio Manso**<br>
[LinkedIn](https://www.linkedin.com/in/sergiomanso/)
