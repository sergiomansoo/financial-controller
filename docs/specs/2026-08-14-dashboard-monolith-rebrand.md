# Spec — Rebranding Monolith do Dashboard

**Status:** aprovado para implementação
**Branch:** `feature/financial-controller-rebrand`
**Escopo:** aparência e composição do dashboard; sem alteração de rotas, autenticação, filtros ou contratos de API.

## Direção visual

Usar a referência fornecida como direção estética, não como cópia de marca: tema monocromático premium e compacto, com superfícies quase pretas, tipografia técnica e números de alta legibilidade. Não usar gradientes, glassmorphism, ícones em círculos ou cores decorativas. As cores semânticas de receita/despesa continuam disponíveis somente onde comunicam significado financeiro.

### Tokens

| Token | Valor | Uso |
| --- | --- | --- |
| `--monolith-bg` | `#000000` | página e área principal |
| `--monolith-sidebar` | `#0D0D0D` | sidebar e topo |
| `--monolith-panel` | `#121212` | cards e painéis |
| `--monolith-control` | `#1C1C1E` | inputs, selects e badges |
| `--monolith-border` | `#2C2C2E` | bordas e divisores |
| `--monolith-text` | `#FFFFFF` | texto e série principal |
| `--monolith-muted` | `#8A8A8D` | labels, eixos e legenda secundária |
| `--monolith-dim` | `#5A5A5C` | série e marcador terciários |

Interface e títulos usam **Inter**; valores monetários, percentuais, datas e eixos usam **JetBrains Mono**. Raio: 8px em controles e cards. Bordas: 1px sólida. Transições apenas de opacity/background/border, entre 150 e 200 ms, respeitando `prefers-reduced-motion`.

## Composição do dashboard

Em desktop (>= 1280px), conteúdo com largura máxima de 1600px, padding de 40px e grid de 12 colunas:

1. Cabeçalho mantém os controles já funcionais de mês e filtro global.
2. Uma faixa com quatro cards iguais: **Rendas**, **Despesas**, **Economias** e **Investimentos**. Cada card tem label superior com ícone Lucide discreto, valor em Inter 36–40px e linha de contexto abaixo em mono. Não incluir ícone em círculo.
3. Abaixo, coluna principal `8/12`: painel “Saldo total” e gráfico de evolução mensal de renda e despesa, com linhas branca sólida e cinza tracejada, grade horizontal sutil e legenda acessível. Preservar o filtro existente.
4. Coluna lateral `4/12`: “Detalhamento de despesas”, usando donut somente para até cinco categorias e lista textual com valor absoluto e porcentagem; quando houver mais categorias, manter a lista/barras como leitura principal. A ação existente para ver transações deve permanecer.
5. Faixa inferior: transações recentes/insights existentes na coluna principal e metas na lateral. Metas de economia usam barras brancas em fundo escuro, poupado/alvo e porcentagem; limites mensais em risco preservam alertas semânticos.

Em < 1280px, as áreas passam a uma coluna; em < 768px, KPIs ficam em grade 2×2. Sem overflow horizontal, exceto tabelas já declaradas responsivas. A sidebar conserva suas rotas e interações; apenas recebe os novos tokens visuais.

## Dados e comportamento

Não alterar o backend. Reutilizar os dados atuais:

- Rendas e despesas: `DashboardData.totals`.
- Evolução e categorias: `monthlyEvolution` e `byCategory` filtrados como hoje.
- Economias: agregação no cliente de `GET /savings-goals?month=YYYY-MM`, usando valores poupados das metas, identificada como “Economias”.
- Investimentos: chamada já disponível de total de transações com `type=INVESTMENT`; quando não houver dado, mostrar valor monetário zero, não inventar percentual.
- Metas e limites: fontes atuais, sem modificar suas ações.

Rotas, autenticação, upload, filtros globais, paginação e regras de categoria permanecem inalterados.

## Estados e acessibilidade

- Skeleton respeita a geometria de KPIs, gráfico e painéis.
- Estados vazio/erro conservam cabeçalho e layout, com ação de recuperação existente.
- Regiões de gráfico têm título, resumo textual e legenda. Linhas não dependem só da cor: renda é sólida, despesa tracejada.
- Foco de teclado permanece visível; botões icon-only têm `aria-label`; contraste de texto normal é >= 4.5:1.

## Critérios de aceite

- Dashboard apresenta os quatro KPIs exigidos e a composição visual 8/12 + 4/12 em desktop.
- Design usa tokens monocromáticos, Inter e JetBrains Mono; não há gradientes ou novos acentos decorativos.
- Filtros e rotas existentes continuam funcionando e os testes de loading, erro, vazio e filtros seguem verdes.
- Responsividade sem overflow em 375px, 768px, 1024px e 1440px.
- `npm test`, `npm run build` e `git diff --check` passam; revisão QA não aponta Critical/Important.
