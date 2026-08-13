# QA Fixture Guide

All QA fixtures must be invented. Do not use real bank statements, real names, real email addresses, passwords, addresses, account identifiers, card numbers, access tokens, or screenshots containing them.

## Synthetic identities

- `Ava Fixture` — `ava.fixture@example.test`
- `Ben Fixture` — `ben.fixture@example.test`
- Local placeholder password: `Fixture-Only-Password-01!`

These are illustrative test identities only. Replace them with unique `.test` identities if the environment retains data between runs.

## Synthetic CSV examples

The import parser expects five metadata lines, then the Banco Inter header, followed by semicolon-delimited UTF-8 rows. Use values invented solely for testing.

`valid-inter-fixture.csv`:

```csv
Relatório fictício 1
Relatório fictício 2
Relatório fictício 3
Relatório fictício 4
Relatório fictício 5
Data;Histórico;Descrição;Valor;Saldo
10/08/2026;Pix enviado   ;;-45,90;954,10
11/08/2026;Salário sintético;Empregador de teste;1000,00;1954,10
12/08/2026;Mercado de teste;Compra fictícia;-120,00;1834,10
```

Coverage embedded in this file:

- `Pix enviado   ` tests trailing-history trimming.
- The empty cell between semicolons tests an empty description.
- `-45,90` tests negative decimal-comma parsing.
- Re-uploading the exact same file tests duplicate-candidate review.

`invalid-header-fixture.csv`:

```csv
Relatório fictício 1
Relatório fictício 2
Relatório fictício 3
Relatório fictício 4
Relatório fictício 5
Date,History,Description,Amount,Balance
10/08/2026,Pix fictício,, -45.90,954.10
```

This deliberately has a non-Banco-Inter header and comma delimiter; it must be rejected as unsupported, without creating transactions.

## Suggested dashboard and isolation data

- For Ava, create synthetic expenses in at least two categories and an income across two months so the category and evolution charts have non-zero values.
- Set a budget below Ava’s synthetic expense total to test `exceeded=true`.
- For Ben, create a distinct manual transaction and budget; use it only to prove that Ava’s records, rules, and aggregates are not visible or mutable by Ben.
- Use neutral fictional descriptions such as `Mercado de teste`, `Transporte de teste`, and `Assinatura sintética` for category-rule scenarios.

## Evidence hygiene

Before attaching evidence, verify that it contains only the invented data above, a local/disposable URL, and redacted authorization headers. Use placeholders in the QA record until safe evidence is available:

```text
[Screenshot: desktop/mobile, viewport, case ID]
[API/log evidence: method, path, status, redacted body]
```
