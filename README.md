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

2. Inicie um PostgreSQL descartável para desenvolvimento local.

   ```powershell
   docker run --name financial-controller-postgres --rm `
     -e POSTGRES_DB=financial_controller `
     -e POSTGRES_USER=financial `
     -e POSTGRES_PASSWORD=change-me-local `
     -p 5432:5432 postgres:16
   ```

3. Em outro terminal, configure e inicie a API. O segredo JWT deve ter pelo menos 32 caracteres e nunca deve ser enviado ao Git.

   ```powershell
   Set-Location backend
   $env:DATABASE_URL='jdbc:postgresql://localhost:5432/financial_controller'
   $env:DATABASE_USERNAME='financial'
   $env:DATABASE_PASSWORD='change-me-local'
   $env:JWT_SECRET='local-development-secret-with-at-least-32-characters'
   $env:CORS_ALLOWED_ORIGIN='http://localhost:5173'
   .\mvnw.cmd spring-boot:run
   ```

   O Flyway aplica automaticamente as quatro migrações existentes na primeira inicialização.

4. Em um terceiro terminal, inicie o frontend.

   ```powershell
   Set-Location frontend
   npm ci
   $env:VITE_API_URL='http://localhost:8080/api/v1'
   npm run dev
   ```

5. Abra `http://localhost:5173`, crie uma conta fictícia e siga o fluxo: importar extrato, revisar categorias, definir orçamento e consultar o dashboard.

## Configuração

O backend não possui credenciais de produção embutidas. Estas variáveis são obrigatórias, salvo indicação contrária:

| Variável | Exemplo local | Descrição |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/financial_controller` | URL JDBC do PostgreSQL. |
| `DATABASE_USERNAME` | `financial` | Usuário do banco. |
| `DATABASE_PASSWORD` | `change-me-local` | Senha do banco. |
| `JWT_SECRET` | valor aleatório com 32+ caracteres | Chave de assinatura JWT. |
| `JWT_EXPIRATION_MS` | `86400000` | Opcional; duração do token em milissegundos. |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | Opcional; única origem liberada para a API. |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Opcional no frontend; base da API. |

Para outro ambiente, substitua todas as credenciais de exemplo e configure `CORS_ALLOWED_ORIGIN` com a origem pública exata do frontend.

### Assistente financeiro (Groq)

O assistente usa permanentemente o modelo `openai/gpt-oss-20b`. Esse modelo e definido pelo backend e nao deve ser alterado por variavel de ambiente.

Para executar a API localmente com o assistente, defina uma chave criada para o seu ambiente no console da Groq no mesmo terminal do backend. Nao copie uma chave real para arquivos, commits, capturas de tela ou evidencias de QA.

```powershell
# backend local somente; nao versione este valor
$env:GROQ_API_KEY='cole-a-chave-gerada-no-console-groq'
.\mvnw.cmd spring-boot:run
```

No Render, cadastre `GROQ_API_KEY` como variavel de ambiente secreta do **servico de backend**. Nunca a cadastre no ambiente estatico do frontend nem como variavel `VITE_*`, pois valores expostos ao frontend podem ser vistos no navegador. Mantenha tambem as demais credenciais do backend (banco e JWT) como segredos do servico correspondente.

O assistente esta disponivel em `POST /api/v1/assistant/chat` e exige JWT. Para responder a pergunta, o backend envia a Groq somente o contexto financeiro autenticado e limitado que for necessario para a analise do mes selecionado, junto do historico recente da conversa. Ele nao recebe senhas, JWTs ou a chave da Groq e nao cria nem altera transacoes, categorias, regras ou orcamentos. Evite inserir informacoes sensiveis que nao sejam necessarias para a sua pergunta financeira.

Quando a chave nao esta configurada, a Groq esta indisponivel ou o plano gratuito aplica limite de taxa, a API pode responder `503`. Essa e uma condicao temporaria: mantenha a pergunta e tente novamente mais tarde; nunca exponha ou registre o valor da chave para investigar o problema.

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
| `POST` | `/api/v1/imports` | Importa `multipart/form-data` com o campo `file`. |
| `GET` | `/api/v1/transactions?month=YYYY-MM` | Lista transações do mês. |
| `POST` | `/api/v1/transactions` | Cria lançamento manual com `date`, `description`, `amount`, `categoryId` e `type`. |
| `PATCH` | `/api/v1/transactions/{id}/category` | Atualiza categoria com `{ "categoryId": 1, "learn": true }`. |
| `GET` | `/api/v1/budgets?month=YYYY-MM` | Lista orçamentos do mês. |
| `PUT` | `/api/v1/budgets/{categoryId}?month=YYYY-MM` | Cria ou atualiza orçamento com `{ "limit": 500.00 }`. |
| `GET` | `/api/v1/dashboard?month=YYYY-MM` | Retorna gastos por categoria, série mensal e orçamentos. |

O endpoint `POST /api/v1/assistant/chat` responde perguntas sobre o contexto financeiro do usuario autenticado e nao altera registros.

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

Na verificação final deste projeto, a suíte do backend executou 30 testes sem falhas; o frontend executou 24 testes sem falhas e gerou o build de produção. O Vite pode emitir um aviso não bloqueante sobre o tamanho do bundle Recharts.

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
| Frontend recebe erro de CORS | Confira se `CORS_ALLOWED_ORIGIN` corresponde exatamente à URL aberta no navegador. |
| Importação rejeitada | Garanta UTF-8, `;`, metadados, linha em branco e o cabeçalho Banco Inter exato. |
| Dashboard vazio | Crie/importa lançamentos no mesmo mês selecionado e defina um orçamento para a categoria. |
| Erro de autenticação | Faça login novamente e confirme que o frontend usa a mesma URL da API configurada em `VITE_API_URL`. |

## Convenções de contribuição

Cada incremento coeso deve passar seus testes antes de ser registrado. Os commits usam Conventional Commits em inglês, por exemplo `feat(frontend): visualize budgets and spending` ou `fix(api): allow configured frontend origin`. Não inclua segredos nem dados reais em commits, issues ou evidências de QA.
