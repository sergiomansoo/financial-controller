import { useEffect, useState } from 'react'
import { AlertCircle, Import as ImportIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { LedgerCharts } from '../components/LedgerCharts'
import { getDashboard } from '../lib/api'
import { useMovementFilter } from '../lib/movement-filter'
import type { DashboardData } from '../types/api'

const currentMonth = () => new Date().toISOString().slice(0, 7)
const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function DashboardPage() {
  const [month, setMonth] = useState(currentMonth); const [data, setData] = useState<DashboardData | null>(null); const [error, setError] = useState(false); const [attempt, setAttempt] = useState(0); const { filter } = useMovementFilter()
  useEffect(() => { let cancelled = false; setData(null); setError(false); getDashboard(month, filter).then((response) => { if (!cancelled) setData(response) }).catch(() => { if (!cancelled) setError(true) }); return () => { cancelled = true } }, [month, filter, attempt])
  if (error) return <section className="ledger-page"><div className="ledger-alert" role="alert"><AlertCircle size={18} />Não foi possível carregar o painel.<button onClick={() => setAttempt((value) => value + 1)}>Tentar novamente</button></div></section>
  if (!data) return <section className="ledger-page"><header className="page-title"><h1>Visão geral</h1><Month value={month} onChange={setMonth} /></header><div className="ledger-skeleton" role="status">Carregando painel…</div></section>
  const totals = data.totals ?? { income: 0, expense: 0, balance: 0 }
  const largestExpense = totals.largestExpense ?? (totals.largestExpenseCategory ? { categoryId: 'legacy', categoryName: totals.largestExpenseCategory, amount: totals.largestExpenseAmount ?? 0 } : undefined)
  const largestIncome = totals.largestIncome
  const empty = data.byCategory.length === 0 && data.monthlyEvolution.every((item) => item.income === 0 && item.expense === 0)
  if (empty) return <section className="ledger-page"><header className="page-title"><div><p>Resumo financeiro</p><h1>Visão geral</h1></div><Month value={month} onChange={setMonth} /></header><section className="ledger-panel ledger-empty-state"><ImportIcon size={30} /><h2>Comece importando seu extrato</h2><p>Não há movimentações neste período.</p><Link className="ledger-button" to="/importar">Importar extrato</Link></section></section>
  const risks = data.budgets.filter((budget) => budget.exceeded || budget.spent / Math.max(budget.limit, 1) >= .8)
  return <section className="ledger-page"><header className="page-title"><div><p>Resumo financeiro</p><h1>Visão geral</h1></div><Month value={month} onChange={setMonth} /></header><div className="kpi-strip"><Kpi label="Saldo" value={totals.balance} tone={totals.balance >= 0 ? 'income' : 'expense'} />{filter !== 'expense' && <Kpi label="Receitas" value={totals.income} tone="income" />}{filter === 'income' && <Highlight label="Maior receita" highlight={largestIncome} />}{filter !== 'income' && <Kpi label="Total gasto no mês" value={totals.expense} tone="expense" />}{filter !== 'income' && <Highlight label="Maior despesa" highlight={largestExpense} />}</div>{filter === 'both' && <section className="ledger-panel" aria-label="Destaques do período"><Highlight label="Maior receita" highlight={largestIncome} /></section>}<section className="metric-indicators" aria-label="Indicadores financeiros"><Metric label="Salário comprometido" value={totals.salaryCommittedPercent ?? 0} /><Metric label="Recebido investido" value={totals.receivedInvestedPercent ?? 0} /></section><LedgerCharts categories={data.byCategory} evolution={data.monthlyEvolution} /><RiskList risks={risks} /></section>
}
function Month({ value, onChange }: { value: string; onChange: (value: string) => void }) { return <label>Mês<input type="month" value={value} onChange={(event) => onChange(event.target.value)} /></label> }
function Kpi({ label, value, caption, tone }: { label: string; value: number | string; caption?: string; tone?: string }) { return <article className={`kpi ${tone ?? ''}`}><span>{label}</span><strong>{typeof value === 'number' ? currency.format(value) : value}</strong>{caption && <small>{caption}</small>}</article> }
function Highlight({ label, highlight }: { label: string; highlight?: { categoryName: string; amount: number } }) { return <Kpi label={label} value={highlight?.categoryName ?? '—'} caption={highlight ? currency.format(Math.abs(highlight.amount)) : undefined} /> }
function Metric({ label, value }: { label: string; value: number }) { return <article className="ledger-panel"><span>{label}</span><strong>{new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)}%</strong></article> }
function RiskList({ risks }: { risks: DashboardData['budgets'] }) { return <section className="ledger-panel"><h2>Metas em risco</h2>{risks.length === 0 ? <p className="ledger-empty">Sem metas em risco.</p> : <ul className="risk-list">{risks.slice(0, 3).map((budget) => <li key={budget.categoryId}><strong>{budget.categoryName}</strong><span>{currency.format(budget.spent)} de {currency.format(budget.limit)}</span></li>)}</ul>}</section> }
