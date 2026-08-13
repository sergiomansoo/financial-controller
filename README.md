# Financial Controller

Aplicação full-stack para importar extratos CSV do Banco Inter, classificar lançamentos, acompanhar orçamentos mensais e visualizar a evolução financeira. O projeto foi pensado para execução local com dados fictícios durante desenvolvimento e QA.

## Recursos

- Cadastro e login com JWT e senhas protegidas por BCrypt.
- Categorias de sistema e regras de categorização aprendidas por usuário.
- Importação de CSV Banco Inter em UTF-8, inclusive campos entre aspas e descrições multilinha.
- Detecção de possíveis duplicidades sem descartar o lançamento: o item fica marcado para revisão.
- Lançamentos manuais dos tipos `EXPENSE`, `INCOME` e `INVESTMENT`.
- Revisão de categoria com opção **Learn** para reaproveitar a regra em lançamentos futuros.
- Orçamentos por categoria e mês, com alerta de estouro.
- Dashboard responsivo com gráficos de gasto por categoria e evolução de seis meses; os gráficos possuem tabelas textuais para leitores de tela.
- Navegação por rotas reais: dashboard, transações, importação, categorias, metas e configurações.
- Filtro persistente de receitas, despesas ou ambos, aplicado ao dashboard e à tabela de transações.
- Tema escuro inspirado em ledger, com gráficos Recharts, tabelas acessíveis e ações de transação.
- Isolamento de todos os dados por usuário autenticado.

## Arquitetura

| Camada | Tecnologia | Responsabilidade |
| --- | --- | --- |
| Frontend | React 19, TypeScript, Vite, Tailwind e Recharts | Autenticação, importação, revisão, formulários, gráficos e orçamentos. |
| Backend | Java 21, Spring Boot 3, Spring Security, JPA e Flyway | API REST, JWT, regras de negócio, validação e migrações. |
| Banco de dados | PostgreSQL | Dados de usuários, categorias, transações, regras e orçamentos. |
| Testes | Vitest/Testing Library e JUnit/MockMvc/H2 | Comportamentos do frontend e integrações da API. |

```text
frontend (http://localhost:5173)
        │  HTTP + Bearer JWT
        ▼
backend  (http://localhost:8080/api/v1)
        │  JPA + Flyway
        ▼
PostgreSQL
```

## Pré-requisitos

- Java 21 ou superior compatível.
- Node.js `>= 24.15.0` e npm.
- PostgreSQL 16+ ou Docker para iniciá-lo localmente.

## Início rápido

1. Clone o repositório e entre nele.

   ```powershell
   git clone https://github.com/sergiomansoo/financial-controller.git
   Set-Location financial-controller
   ```

2. Garanta que exista um PostgreSQL em `localhost:5432` com o banco `financial_controller`. Se o seu PostgreSQL local já usa `postgres` / `postgres`, crie apenas o banco uma vez:

   ```powershell
   psql -U postgres -c "CREATE DATABASE financial_controller;"
   ```

   Alternativamente, inicie um PostgreSQL descartável para desenvolvimento local.

   ```powershell
   docker run --name financial-controller-postgres --rm `
     -e POSTGRES_DB=financial_controller `
     -e POSTGRES_USER=postgres `
     -e POSTGRES_PASSWORD=postgres `
     -p 5432:5432 postgres:16
   ```

3. Em um terminal, configure e inicie a API. Execute estes comandos a partir da raiz do repositório; o segredo JWT deve ter pelo menos 32 caracteres e nunca deve ser enviado ao Git.

   ```powershell
   Set-Location backend
   $env:DATABASE_URL='jdbc:postgresql://localhost:5432/financial_controller'
   $env:DATABASE_USERNAME='postgres'
   $env:DATABASE_PASSWORD='postgres'
   $env:JWT_SECRET='local-development-secret-with-at-least-32-characters'
   $env:APP_CORS_ALLOWED_ORIGIN='http://localhost:5173'
   .\mvnw.cmd spring-boot:run
   ```

   O Flyway aplica automaticamente as quatro migrações existentes na primeira inicialização.

4. Em um segundo terminal, inicie o frontend. Mantenha a API em execução no primeiro terminal.

   ```powershell
   Set-Location frontend
   npm ci
   $env:VITE_API_URL='http://localhost:8080/api/v1'
   npm run dev
   ```

5. Abra `http://localhost:5173` (não misture `localhost` e `127.0.0.1`, pois CORS exige a origem exata), crie uma conta e siga o fluxo: importar extrato, revisar categorias, definir metas e consultar o dashboard.

### Comandos de execução direta

Para abrir o projeto novamente, não é preciso nenhum comando especial do Codex. Em dois terminais PowerShell, dentro da pasta clonada, use os dois blocos acima: o backend responde em `http://localhost:8080/api/v1` e o frontend em `http://localhost:5173`.

Para encerrar, pressione `Ctrl+C` em cada terminal. Caso a porta 8080 ou 5173 já esteja ocupada, encerre o processo anterior antes de iniciar outro.

## Configuração

O backend não possui credenciais de produção embutidas. Estas variáveis são obrigatórias, salvo indicação contrária:

| Variável | Exemplo local | Descrição |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/financial_controller` | URL JDBC do PostgreSQL. |
| `DATABASE_USERNAME` | `postgres` | Usuário do banco local usado no guia. |
| `DATABASE_PASSWORD` | `postgres` | Senha do banco local usado no guia. |
| `JWT_SECRET` | valor aleatório com 32+ caracteres | Chave de assinatura JWT. |
| `JWT_EXPIRATION_MS` | `86400000` | Opcional; duração do token em milissegundos. |
| `APP_CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | Opcional; única origem liberada para a API. |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Opcional no frontend; base da API. |

Para outro ambiente, substitua todas as credenciais de exemplo e configure `APP_CORS_ALLOWED_ORIGIN` com a origem pública exata do frontend.

## Formato do CSV Banco Inter

O importador aceita arquivo UTF-8 delimitado por ponto e vírgula com quatro linhas de metadados, uma linha em branco e o cabeçalho abaixo:

```csv
Extrato Conta Corrente
Conta;00000000-0
Período;01/08/2026 a 31/08/2026
Saldo;1000,00

Data Lançamento;Histórico;Descrição;Valor;Saldo
10/08/2026;Pix enviado;;-45,90;954,10
```

Regras relevantes:

- Valores usam vírgula decimal e são persistidos como `BigDecimal`.
- O histórico é normalizado sem espaços ao final; a descrição pode ficar vazia.
- Linhas com a mesma tupla data, histórico, descrição e valor são mantidas e marcadas como `needsReview`.
- Categorias aprendidas pelo usuário têm prioridade sobre palavras-chave do sistema; itens sem correspondência vão para **Outros**.
- Arquivos fora do formato recebem resposta `400` com erro padronizado e não criam transações.

Há exemplos exclusivamente sintéticos em [docs/fixtures/README.md](docs/fixtures/README.md) e um roteiro de QA em [docs/qa-test-plan.md](docs/qa-test-plan.md).

## API REST

Todas as rotas, exceto autenticação, exigem `Authorization: Bearer <token>`.

| Método | Rota | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Cria uma conta e devolve JWT. |
| `POST` | `/api/v1/auth/login` | Autentica uma conta existente. |
| `GET` | `/api/v1/categories` | Lista categorias acessíveis ao usuário. |
| `POST` | `/api/v1/categories` | Cria uma categoria do usuário. |
| `DELETE` | `/api/v1/categories/{id}` | Exclui uma categoria do usuário quando ela não possui referências. |
| `GET/POST/DELETE` | `/api/v1/category-rules` | Lista, cria ou remove regras de palavra-chave. |
| `POST` | `/api/v1/imports` | Importa `multipart/form-data` com o campo `file`. |
| `GET` | `/api/v1/transactions?month=YYYY-MM&page=0&size=10` | Lista transações paginadas, com filtros de categoria, data e tipo. |
| `POST` | `/api/v1/transactions` | Cria lançamento manual com `date`, `description`, `amount`, `categoryId` e `type`. |
| `PATCH` | `/api/v1/transactions/{id}/category` | Atualiza categoria com `{ "categoryId": 1, "learn": true }`. |
| `GET` | `/api/v1/budgets?month=YYYY-MM` | Lista orçamentos do mês. |
| `PUT` | `/api/v1/budgets/{categoryId}?month=YYYY-MM` | Cria ou atualiza orçamento com `{ "limit": 500.00 }`. |
| `GET` | `/api/v1/dashboard?month=YYYY-MM&filter=both` | Retorna KPIs, gastos por categoria, série mensal e metas; `filter` aceita `both`, `income` ou `expense`. |

Erros seguem o formato:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed.",
  "fieldErrors": { "month": "..." }
}
```

## Testes e qualidade

Execute os comandos a partir dos respectivos diretórios:

```powershell
# backend
Set-Location backend
.\mvnw.cmd test

# frontend
Set-Location ../frontend
npm ci
npm test
npm run build
```

Na verificação final deste projeto, a suíte do backend executou 38 testes sem falhas; o frontend executou 45 testes sem falhas e gerou o build de produção. O Vite pode emitir um aviso não bloqueante sobre o tamanho do bundle Recharts.

## Organização do repositório

```text
backend/
  src/main/java/              # API e regras de negócio
  src/main/resources/db/      # migrações Flyway
  src/test/                   # testes de integração e unidade
frontend/
  src/components/             # formulários, tabelas, gráficos e orçamento
  src/pages/                  # rotas de autenticação e dashboard
  src/lib/                    # cliente HTTP e sessão
  src/types/                  # contratos TypeScript
docs/
  fixtures/                   # dados de QA sintéticos
  qa-test-plan.md             # roteiro manual e evidências esperadas
```

## Segurança e privacidade

- Use somente dados fictícios em desenvolvimento e QA; não versionar extratos, tokens, senhas ou dados bancários reais.
- JWTs são assinados no servidor e as rotas protegidas filtram dados pelo usuário autenticado.
- Senhas são armazenadas com BCrypt.
- O CORS permite apenas a origem configurada; mantenha-a restrita no ambiente publicado.
- O frontend mantém a sessão localmente para este MVP. Em um ambiente com maior exposição a XSS, avalie migrar a estratégia de sessão para cookies `HttpOnly` com proteção CSRF adequada.

## Solução de problemas

| Sintoma | Verificação |
| --- | --- |
| API não inicia | Confirme `DATABASE_*` e `JWT_SECRET`; confira se PostgreSQL está acessível. |
| Frontend recebe erro de CORS | Confira se `APP_CORS_ALLOWED_ORIGIN` corresponde exatamente à URL aberta no navegador. |
| Importação rejeitada | Garanta UTF-8, `;`, metadados, linha em branco e o cabeçalho Banco Inter exato. |
| Dashboard vazio | Crie/importa lançamentos no mesmo mês selecionado e defina um orçamento para a categoria. |
| Erro de autenticação | Faça login novamente e confirme que o frontend usa a mesma URL da API configurada em `VITE_API_URL`. |

## Convenções de contribuição

Cada incremento coeso deve passar seus testes antes de ser registrado. Os commits usam Conventional Commits em inglês, por exemplo `feat(frontend): visualize budgets and spending` ou `fix(api): allow configured frontend origin`. Não inclua segredos nem dados reais em commits, issues ou evidências de QA.
