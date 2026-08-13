import { useEffect, useState } from 'react'
import { AlertCircle, RefreshCw } from 'lucide-react'
import { getDashboard } from '../lib/api'
import { useMovementFilter } from '../lib/movement-filter'
import type { DashboardData } from '../types/api'

const currentMonth = () => new Date().toISOString().slice(0, 7)
const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function DashboardPage() {
  const [month, setMonth] = useState(currentMonth)
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const { filter } = useMovementFilter()

  useEffect(() => {
    let cancelled = false
    setData(null); setError(false)
    getDashboard(month, filter).then((response) => {
      if (!cancelled) setData(response)
    }).catch(() => {
      if (!cancelled) setError(true)
    })
    return () => { cancelled = true }
  }, [month, filter, attempt])

  if (error) return <section className="ledger-page"><div className="ledger-alert" role="alert"><AlertCircle size={18} />Não foi possível carregar o painel.</div><button className="ledger-button" onClick={() => setAttempt((value) => value + 1)}><RefreshCw size={16} />Tentar novamente</button></section>
  if (!data) return <section className="ledger-page"><header className="page-title"><h1>Visão geral</h1><label>Mês<input type="month" value={month} onChange={(event) => setMonth(event.target.value)} /></label></header><div className="ledger-skeleton" role="status">Carregando painel…</div></section>

  const totals = data.totals ?? { income: 0, expense: 0, balance: 0 }
  const largest = Math.max(...data.byCategory.map((item) => Math.abs(item.spent)), 1)
  const risks = data.budgets.filter((budget) => budget.exceeded || budget.spent / Math.max(budget.limit, 1) >= .8)
  return <section className="ledger-page">
    <header className="page-title"><div><p>Resumo financeiro</p><h1>Visão geral</h1></div><label>Mês<input type="month" value={month} onChange={(event) => setMonth(event.target.value)} /></label></header>
    <div className="kpi-strip">
      <Kpi label="Receitas" value={totals.income} tone="income" />
      <Kpi label="Total gasto no mês" value={totals.expense} tone="expense" />
      <Kpi label="Saldo" value={totals.balance} tone={totals.balance >= 0 ? 'income' : 'expense'} />
      <Kpi label="Maior despesa" value={totals.largestExpenseCategory ?? '—'} caption={totals.largestExpenseAmount ? currency.format(totals.largestExpenseAmount) : undefined} />
    </div>
    <section className="ledger-panel"><h2>Gastos por categoria</h2>{data.byCategory.length === 0 ? <p className="ledger-empty">Nenhuma movimentação encontrada neste período.</p> : <><div className="bar-chart" aria-hidden="true">{data.byCategory.map((item) => <div className="bar-row" key={item.categoryId}><span>{item.categoryName}</span><div className="bar-track"><i style={{ width: `${Math.abs(item.spent) / largest * 100}%` }} /></div><b>{currency.format(item.spent)}</b></div>)}</div><table className="sr-only"><caption>Gastos por categoria</caption><tbody>{data.byCategory.map((item) => <tr key={item.categoryId}><th>{item.categoryName}</th><td>{currency.format(item.spent)}</td></tr>)}</tbody></table></>}</section>
    <section className="ledger-panel"><h2>Evolução mensal</h2><table><caption className="sr-only">Evolução mensal de receitas e despesas</caption><thead><tr><th>Mês</th><th>Receitas</th><th>Despesas</th></tr></thead><tbody>{data.monthlyEvolution.map((item) => <tr key={item.month}><td>{item.month}</td><td className="income">{currency.format(item.income)}</td><td className="expense">{currency.format(item.expense)}</td></tr>)}</tbody></table></section>
    <section className="ledger-panel"><h2>Metas em risco</h2>{risks.length === 0 ? <p className="ledger-empty">Sem metas em risco.</p> : <ul className="risk-list">{risks.slice(0, 3).map((budget) => <li key={budget.categoryId}><strong>{budget.categoryName}</strong><span>{currency.format(budget.spent)} de {currency.format(budget.limit)}</span></li>)}</ul>}</section>
  </section>
}
function Kpi({ label, value, caption, tone }: { label: string; value: number | string; caption?: string; tone?: string }) { return <article className={`kpi ${tone ?? ''}`}><span>{label}</span><strong>{typeof value === 'number' ? currency.format(value) : value}</strong>{caption && <small>{caption}</small>}</article> }
