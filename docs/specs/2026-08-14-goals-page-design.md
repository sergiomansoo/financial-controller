# Redesign da página Metas

## Direção

Transformar `/metas` de uma sequência de formulários em um livro-caixa de compromissos futuros: leitura imediata do mês no topo, dois capítulos independentes — gasto e poupança — e uma única ação de contribuição por objetivo. Não usar superfícies genéricas repetidas: o agrupamento vem de títulos, réguas, linhas divisórias e espaço negativo. Manter o fundo ink `#14161B`, superfícies ledger `#1C1F26/#242832`, bordas `#2E323C`, Fraunces para títulos e IBM Plex Sans/Mono para dados.

## Hierarquia e layout desktop (>= 1024px)

1. **Cabeçalho da página** — largura máxima de 1.300px, margem inferior de 32px. À esquerda: sobrancelha `PLANEJAMENTO` em Plex Sans 12px/600, tracking 0,08em e cor muted; título `Metas` em Fraunces 36px/700; subtítulo de uma linha em Plex Sans 14px muted. À direita: seletor `Mês` de 152px. Nenhuma métrica neste cabeçalho.
2. **Resumo fino** — faixa com três leituras, sem contêiner externo: `Limites acompanhados`, `Metas de poupança`, `Aporte no mês`. Valores em Plex Mono 18px/600, rótulos 12px muted; divisores verticais `#2E323C`. É um resumo, não três cards.
3. **Limites mensais de gastos** — seção à esquerda (7/12 da grade), iniciada por título Fraunces 22px e texto “Para não extrapolar em agosto”. Cada categoria é uma linha de ledger de 72px: nome; `R$ gasto de R$ limite`; régua de 8px; botão textual `Ajustar`. Não repetir `h2` em cada linha. A régua usa green (`#2FA88A`) abaixo de 80%, amber (`#D9A441`) de 80% a 100%, burgundy (`#A6435C`) acima de 100%; largura visual limitada a 100%, mas o texto mostra o excedente.
4. **Objetivos de poupança** — coluna à direita (5/12), separada por divisor vertical. Cabeçalho com título Fraunces 22px, contador de objetivos e botão primário compacto `Novo objetivo`. Cada objetivo é uma entrada de ledger, não um card elevado: nome e data-alvo; total poupado / alvo; régua global; metadados `Agosto: R$ X de R$ Y`; botão primário `Registrar aporte` e ação discreta `Ver detalhes`. Usar no máximo três objetivos no painel; um link `Ver todos os objetivos` quando necessário.
5. **Objetivos completos** — abaixo das duas colunas, quando houver mais de três, lista de uma coluna com o mesmo formato, nunca mosaico de cards. Separar entradas por borda superior.

Gutters: 24px entre colunas e seções; padding da página 32px; padding interno de linhas 16px vertical / 0 horizontal. Cantos de 6px apenas em controles e 10px apenas quando um bloco de erro ou formulário precisar de superfície própria.

## Layout mobile (< 768px)

- Padding de página 20px; cabeçalho empilhado; seletor de mês ocupa 100% e mantém altura de 40px.
- Resumo vira lista vertical de três linhas de 48px, com divisor horizontal; não vira cartões.
- Ordem: resumo, **Limites mensais de gastos**, **Objetivos de poupança**. Não há duas colunas.
- Cada linha de gasto: primeira linha nome + percentual; segunda linha valores; terceira régua; ação `Ajustar` alinhada ao fim. Altura livre, mínimo 112px.
- Objetivo: nome/data, total e régua, depois uma linha de ações; `Registrar aporte` ocupa largura disponível e `Ver detalhes` fica como botão de ícone com rótulo acessível. Inputs nunca ficam lado a lado.
- O painel/modal de aporte usa folha inferior com `max-height: 90vh`, scroll interno e botão de confirmação fixado ao rodapé. Em telas maiores, usar diálogo modal de 420px.

## Componentes e estados

### `GoalsOverview`

Recebe mês, `Budget[]` e `SavingsGoal[]`; calcula contagens e `aporteNoMes = sum(savedAmount)`. Não calcula progresso do servidor no cliente além de formatar percentuais já recebidos.

### `SpendingLimitsSection` e `SpendingLimitRow`

- Estado normal: linha, régua e ação `Ajustar`.
- Ajuste: troca apenas a coluna de limite por campo monetário com prefixo visual `R$`, `Cancelar` e `Salvar`; ao salvar, atualização otimista do valor/régua e anúncio de sucesso. Falha restaura o valor anterior e exibe alerta inline.
- Vazio: ícone Lucide `WalletCards` em outline, “Ainda não há limites para agosto” e orientação curta; não apresentar bloco decorativo grande.

### `SavingsGoalsSection` e `SavingsGoalRow`

- Estado normal: nome, data-alvo, total, progresso global e mensal, ações.
- **Decisão de contrato:** `Data-alvo` permanece obrigatória. O produto já trata a data como obrigatória e não há autorização para alterar o contrato/backend nesta entrega. O campo usa `required`, não aceita valor vazio e a criação só envia `POST /savings-goals` após validação de nome, valor-alvo e data-alvo.
- **Reconciliação da implementação atual:** substituir o formulário de criação permanentemente inline por um diálogo progressivo acionado por `Novo objetivo`; o formulário não pode permanecer visível na página. O diálogo contém `Nome`, `Valor-alvo` e `Data-alvo` (obrigatórios), com `Cancelar` e `Criar objetivo`.
- Vazio: “Crie o primeiro objetivo para dar destino à sua poupança” e botão `Novo objetivo`.
- Carregamento: quatro linhas skeleton com mesma geometria das linhas finais; sem texto piscante.
- Erro de carregamento: bloco compacto com borda burgundy, ícone `AlertCircle`, mensagem e `Tentar novamente`.

### Fluxo “Registrar aporte”

1. Acionar `Registrar aporte` em “Entrada do carro”; diálogo/folha abre com título “Aporte em Entrada do carro”, período “Agosto de 2026”, contexto “Faltam R$ 10.000,00 para o objetivo” e campo obrigatório `Valor do aporte`. O diálogo usa `role="dialog"`, `aria-modal="true"`, `aria-labelledby` apontando para o título e `aria-describedby` apontando para o contexto e a prévia de saldo.
2. O campo aceita moeda BRL, teclado decimal, mínimo `0,01`, duas casas, e mostra uma prévia viva: `Após este aporte: faltam R$ 9.500,00` para R$ 500,00. Não confundir aporte com valor planejado.
3. Confirmar chama o contrato de meta mensal com `savedAmount = savedAmount atual + aporte`; `plannedAmount` permanece inalterado. Enquanto salva, desabilitar cancelar/confirmar e mostrar “Registrando…”.
4. Em sucesso, fechar o diálogo, atualizar a linha sem recarregar: `overallSavedAmount`, `overallProgressPercent`, `savedAmount` do mês, `progressPercent` e o resumo “Aporte no mês”. Anunciar: “Aporte de R$ 500,00 registrado em Entrada do carro. Faltam R$ 9.500,00.”
5. Em erro, manter o diálogo, preservar o valor digitado, mostrar `role=alert` e permitir nova tentativa. Não aplicar atualização otimista irreversível.

`Ver detalhes` leva a uma visão do objetivo com histórico mensal; se ela não existir ainda, manter a ação fora da interface até existir — não exibir botão inerte.

## Tipografia, cor e régua

- Títulos de seção: Fraunces 22px/600; título de objetivo: Plex Sans 16px/600; rótulos: Plex Sans 12px/600; números e moeda: Plex Mono 14px/500, totais 20px/600.
- Texto primário `#EDEAE3`; secundário `#8B8F98`. Green exclusivamente para progresso saudável e CTA principal; burgundy para excesso/erro/destruição; amber para atenção. `#4C8BF5` apenas para foco e links de navegação.
- Réguas: trilho `#14161B`, 8px de altura, raio 999px. Exibir percentuais em texto; cor nunca é o único sinal. Sem gradientes, sombras, blur ou cores Tailwind semânticas.
- Controles: 40px de altura mínima, borda `#2E323C`, fundo ink, raio 6px. Botão destrutivo é textual/burgundy e fica em menu de ações ou confirmação, nunca ao lado do CTA de aporte.

## Acessibilidade e interação

- Um único `h1`; seções são `h2`; nomes de objetivo/categoria são `h3` somente quando necessários para a árvore semântica.
- Cada régua tem `role="progressbar"`, `aria-valuemin="0"`, `aria-valuemax="100"`, `aria-valuenow` limitado a 100 e `aria-valuetext` completo (ex.: “Moradia: R$ 1.800 de R$ 2.000, 90%”).
- Diálogos de criação e aporte: guardar o elemento originador antes de abrir; mover foco inicial para o primeiro campo; conter `Tab`/`Shift+Tab` entre os controles do diálogo; fechar com `Escape` ou Cancelar apenas se não estiver salvando; ao fechar por sucesso, cancelamento ou `Escape`, devolver foco ao botão originador. Usar `role="dialog"`, `aria-modal="true"`, `aria-labelledby` no título e `aria-describedby` no texto de contexto/ajuda e prévia.
- Todas as mensagens assíncronas usam região `aria-live="polite"`; falhas usam `role="alert"`. Estados `hover`, `focus-visible` com outline de 2px `#4C8BF5` e alvo mínimo de 44px.
- Respeitar `prefers-reduced-motion`; atualizações de régua não devem depender de animação. Permitir tabulação em ordem visual e não ocultar texto em ações de ícone.

## Critérios de aceite

- Em 1440px, gastos e poupança ocupam 7/12 e 5/12; em 375px ficam em coluna, sem overflow horizontal nem grids de cards.
- A página mostra claramente duas seções distintas, um seletor de mês e o resumo sem duplicar métricas em cards.
- Gastos e limites são sempre exibidos positivos; progresso >100% usa cor/aviso de excesso e régua em 100%.
- Um aporte de R$ 500 em agosto para “Entrada do carro” cujo restante é R$ 10.000 atualiza para R$ 9.500, recalcula progresso mensal e geral e atualiza o resumo sem recarregar.
- Salvar limite e aporte trata loading, sucesso e erro; falhas não perdem a entrada nem deixam dados inconsistentes.
- `Novo objetivo` abre diálogo; não há formulário de criação inline. Nome, valor-alvo e data-alvo são obrigatórios, e a submissão sem data-alvo é bloqueada com mensagem associada ao campo.
- Testes cobrem vazio/carregamento/erro, responsividade, barras com texto acessível, edição de limite, criação de objetivo e o fluxo de aporte, incluindo erro e sucesso.
- A inspeção visual confirma Fraunces/IBM Plex, tokens ledger definidos, ausência de gradientes e foco teclado visível.

### Checklist de foco dos diálogos

- Ao abrir criação/aporte, o foco vai para o primeiro campo; o leitor anuncia título e descrição.
- `Tab` no último controle volta ao primeiro; `Shift+Tab` no primeiro vai ao último; o foco não alcança a página atrás do diálogo.
- `Escape` e `Cancelar` fecham apenas fora do estado de envio e retornam foco ao botão que abriu o diálogo.
- Após sucesso, o diálogo fecha, o foco retorna ao originador e a região `aria-live` anuncia o resultado; após erro, o foco permanece no diálogo e o alerta é anunciado.
