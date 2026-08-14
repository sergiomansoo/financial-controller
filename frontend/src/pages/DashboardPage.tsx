import { useEffect, useState } from 'react'
import { ArrowDownRight, ArrowUpRight, Landmark, PiggyBank, AlertCircle, Import as ImportIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { LedgerCharts } from '../components/LedgerCharts'
import { getDashboard } from '../lib/api'
import { useMovementFilter } from '../lib/movement-filter'
import type { DashboardData } from '../types/api'
import './DashboardPage.css'

const currentMonth = () => new Date().toISOString().slice(0, 7)
const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function DashboardPage() {
  const [month, setMonth] = useState(currentMonth), [data, setData] = useState<DashboardData | null>(null), [error, setError] = useState(false), [attempt, setAttempt] = useState(0)
  const { filter } = useMovementFilter()
  useEffect(() => { let cancelled = false; setData(null); setError(false); getDashboard(month, filter).then((response) => { if (!cancelled) setData(response) }).catch(() => { if (!cancelled) setError(true) }); return () => { cancelled = true } }, [month, filter, attempt])
  const header = <header className="page-title monolith-header"><div><p>RESUMO FINANCEIRO</p><h1>Visão geral</h1></div><Month value={month} onChange={setMonth} /></header>
  if (error) return <section className="ledger-page monolith-dashboard">{header}<div className="ledger-alert" role="alert"><AlertCircle size={18} />Não foi possível carregar o painel.<button onClick={() => setAttempt((value) => value + 1)}>Tentar novamente</button></div></section>
  if (!data) return <section className="ledger-page monolith-dashboard">{header}<div className="monolith-skeleton" role="status" aria-label="Carregando painel"><span>Carregando painel…</span><i /><i /><i /><i /><i /></div></section>
  const totals = data.totals ?? { income: 0, expense: 0, balance: 0 }
  const largestExpense = totals.largestExpense ?? (totals.largestExpenseCategory ? { categoryId: 'legacy', categoryName: totals.largestExpenseCategory, amount: totals.largestExpenseAmount ?? 0 } : undefined), largestIncome = totals.largestIncome
  const empty = data.byCategory.length === 0 && data.monthlyEvolution.every((item) => item.income === 0 && item.expense === 0)
  if (empty) return <section className="ledger-page monolith-dashboard">{header}<section className="monolith-panel monolith-empty-state"><ImportIcon size={30} /><h2>Comece importando seu extrato</h2><p>Não há movimentações neste período.</p><Link className="ledger-button" to="/importar">Importar extrato</Link></section></section>
  const risks = data.budgets.filter((budget) => budget.exceeded || Math.abs(budget.spent) / Math.max(Math.abs(budget.limit), 1) >= .8)
  return <section className="ledger-page monolith-dashboard" aria-label="Painel financeiro">{header}<div className="monolith-kpi-grid" data-testid="monolith-kpi-grid"><Kpi icon={ArrowUpRight} label="Rendas" value={totals.income} context="Entradas no período" /><Kpi icon={ArrowDownRight} label="Despesas" value={Math.abs(totals.expense)} context="Saídas no período" /><Kpi icon={PiggyBank} label="Economias" value={0} context="Metas poupadas" /><Kpi icon={Landmark} label="Investimentos" value={0} context="Total investido" /></div><Highlights filter={filter} income={largestIncome} expense={largestExpense} /><div className="monolith-layout"><section className="monolith-flow" aria-labelledby="financial-flow-title"><h2 id="financial-flow-title">Saldo total</h2><strong>{currency.format(totals.balance)}</strong><p>Fluxo financeiro mensal</p><LedgerCharts categories={data.byCategory} evolution={data.monthlyEvolution} /></section><aside className="monolith-side"><section className="monolith-panel" aria-label="Detalhamento de despesas"><h2>Detalhamento de despesas</h2><LedgerCharts categories={data.byCategory} evolution={[]} /></section></aside></div><div className="monolith-bottom"><section className="monolith-panel"><h2>Indicadores financeiros</h2><div className="monolith-metrics"><Metric label="Salário comprometido" value={totals.salaryCommittedPercent ?? 0} /><Metric label="Recebido investido" value={totals.receivedInvestedPercent ?? 0} /></div></section><RiskList risks={risks} /></div></section>
}
function Month({ value, onChange }: { value: string; onChange: (value: string) => void }) { return <label className="monolith-month">Mês<input type="month" value={value} onChange={(event) => onChange(event.target.value)} /></label> }
function Kpi({ icon: Icon, label, value, context }: { icon: typeof ArrowUpRight; label: string; value: number; context: string }) { return <article className="monolith-kpi"><span><Icon size={16} aria-hidden />{label}</span><strong>{currency.format(Math.abs(value))}</strong><small>{context}</small></article> }
function Highlights({ filter, income, expense }: { filter: string; income?: { categoryName: string; amount: number }; expense?: { categoryName: string; amount: number } }) { return <section className="monolith-highlights" aria-label="Destaques do período">{filter !== 'income' && <p><span>Maior despesa</span><strong>{expense?.categoryName ?? '—'}</strong>{expense && <small>{currency.format(Math.abs(expense.amount))}</small>}</p>}{filter !== 'expense' && <p><span>Maior receita</span><strong>{income?.categoryName ?? '—'}</strong>{income && <small>{currency.format(Math.abs(income.amount))}</small>}</p>}</section> }
function Metric({ label, value }: { label: string; value: number }) { return <p><span>{label}</span><strong>{new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)}%</strong></p> }
function RiskList({ risks }: { risks: DashboardData['budgets'] }) { return <section className="monolith-panel" aria-label="Metas e limites"><h2>Metas e limites</h2>{risks.length === 0 ? <p className="ledger-empty">Sem metas em risco.</p> : <ul className="risk-list">{risks.slice(0, 3).map((budget) => <li key={budget.categoryId}><strong>{budget.categoryName}</strong><span>{currency.format(Math.abs(budget.spent))} de {currency.format(Math.abs(budget.limit))}</span></li>)}</ul>}</section> }
