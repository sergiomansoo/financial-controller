# Spec — Rebranding Monolith global

## Objetivo

Aplicar a identidade Monolith em todo o frontend — autenticação, shell e todas as rotas — preservando funções, rotas e APIs. O dashboard passa a conter também o painel real **Planos de Economia** da referência.

## Sistema visual obrigatório

- Fundo `#000`; sidebar/topo `#0D0D0D`; painéis `#121212`; controles `#1C1C1E`; bordas `#2C2C2E`.
- Texto principal `#FFF`, secundário `#8A8A8D`, terciário `#5A5A5C`.
- Inter para UI; JetBrains Mono para dinheiro, percentuais, datas e dados tabulares.
- Raio 8px; borda sólida 1px; sem gradientes, blur/glass ou sombras pesadas.
- Botão primário: branco com texto preto; secundário: fundo transparente/borda cinza/texto branco; destrutivo: borda e texto de erro acessíveis; foco branco/alto contraste.
- Inputs, selects, diálogos, tabelas, badges e estados de loading/empty/error usam os mesmos tokens.

## Páginas

- `/login` e `/register`: painel preto, controles escuros, CTA branco e erro legível.
- `/dashboard`: quatro KPIs; fluxo de renda/despesa com linha branca sólida e cinza tracejada; donut monocromático de despesas com lista textual; **Planos de Economia** mostrando soma de `overallSavedAmount` e cada savings goal com barra branca/opacidade, poupado/alvo e percentual; ação para `/metas`.
- `/transacoes`, `/importar`, `/categorias`, `/metas`, `/configuracoes`: aplicar cards, tabelas, formulários, filtros, modais e botões Monolith sem modificar qualquer ação existente.
- Shell/sidebar: manter links, rotas, drawer e comportamento; trocar somente apresentação para os tokens globais.

## Dados e acessibilidade

- Planos de Economia usa `getSavingsGoals(month)` já existente; não mudar backend.
- Donut é complementar à lista com nome, valor e percentual; no máximo cinco segmentos e fallback textual.
- Elementos de ação têm alvo mínimo 44px, foco visível e `aria-label` quando forem só ícone.
- Rendas/despesas não dependem apenas de cor: legenda identifica linha sólida/tracejada.

## Critérios de aceite

- Nenhuma página restante apresenta paleta Ledger anterior em cards, controles ou botões.
- Dashboard renderiza fluxo, detalhamento de despesas e planos reais de economia.
- Rotas, autenticação, importação, filtros, tabelas e metas continuam funcionais.
- Testes focados, suíte frontend, build e `git diff --check` passam; QA não aponta Critical/Important.
