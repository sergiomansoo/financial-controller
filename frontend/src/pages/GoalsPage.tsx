import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createSavingsGoal, deleteSavingsGoal, getBudgets, getSavingsGoals, updateBudget, updateSavingsGoalMonth } from '../lib/api'
import { useAuth } from '../lib/auth'
import type { Budget, SavingsGoal } from '../types/api'

const now = () => new Date().toISOString().slice(0, 7)
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function GoalsPage() {
  const [month, setMonth] = useState(now), [budgets, setBudgets] = useState<Budget[] | null>(null), [goals, setGoals] = useState<SavingsGoal[]>([]), [createError, setCreateError] = useState(''), [loadError, setLoadError] = useState(false), [attempt, setAttempt] = useState(0)
  const { clearSession } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false
    setBudgets(null)
    setLoadError(false)
    Promise.all([getBudgets(month), getSavingsGoals(month)]).then(([nextBudgets, nextGoals]) => {
      if (!cancelled) { setBudgets(nextBudgets); setGoals(nextGoals ?? []) }
    }).catch((error: { status?: number }) => {
      if (cancelled) return
      if (error.status === 401 || error.status === 403) {
        clearSession()
        navigate('/login', { replace: true, state: { message: 'Sua sessão expirou. Entre novamente.' } })
        return
      }
      setBudgets([])
      setGoals([])
      setLoadError(true)
    })
    return () => { cancelled = true }
  }, [month, attempt])

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget, values = new FormData(form)
    try {
      setCreateError('')
      const goal = await createSavingsGoal({ name: String(values.get('name')), targetAmount: Number(values.get('targetAmount')), targetDate: String(values.get('targetDate')) })
      setGoals((current) => [...current, goal])
      form.reset()
    } catch (error) { setCreateError(error instanceof Error ? error.message : 'Não foi possível criar o objetivo.') }
  }

  return <section className="ledger-page"><header className="page-title"><h1>Metas mensais</h1><label>Mês<input type="month" value={month} onChange={(e) => setMonth(e.target.value)} /></label></header>{loadError ? <div role="alert">Não foi possível carregar as metas.<button onClick={() => setAttempt((value) => value + 1)}>Tentar novamente</button></div> : !budgets ? <p role="status">Carregando metas…</p> : <><section><h2>Limites mensais de gastos</h2>{budgets.map((budget) => <BudgetRow budget={budget} month={month} key={budget.categoryId} />)}</section><section className="ledger-panel"><h2>Objetivos de poupança</h2><form className="inline-form" onSubmit={create}><label>Nome do objetivo<input name="name" required /></label><label>Valor-alvo<input name="targetAmount" required type="number" /></label><label>Data-alvo<input name="targetDate" required type="date" /></label><button>Criar objetivo</button></form>{createError && <p role="alert">{createError}</p>}{goals.map((goal) => <GoalRow goal={goal} month={month} key={goal.id} update={(next) => setGoals((current) => current.map((item) => item.id === next.id ? next : item))} remove={(id) => setGoals((current) => current.filter((item) => item.id !== id))} />)}</section></>}</section>
}

function BudgetRow({ budget, month }: { budget: Budget; month: string }) { const [limit, setLimit] = useState(String(Math.abs(budget.limit))), [current, setCurrent] = useState(budget); return <article><h2>{current.categoryName}</h2><label>Limite<input value={limit} onChange={(event) => setLimit(event.target.value)} /></label><button onClick={async () => setCurrent(await updateBudget(current.categoryId, month, Number(limit)))}>Salvar</button></article> }
function GoalRow({ goal, month, update, remove }: { goal: SavingsGoal; month: string; update: (goal: SavingsGoal) => void; remove: (id: string | number) => void }) { const [planned, setPlanned] = useState(String(goal.plannedAmount)), [saved, setSaved] = useState(String(goal.savedAmount)), [error, setError] = useState(''); const save = async () => { try { setError(''); update(await updateSavingsGoalMonth(goal.id, month, Number(planned), Number(saved))) } catch { setError('Não foi possível salvar os valores do objetivo.') } }; const del = async () => { try { setError(''); await deleteSavingsGoal(goal.id); remove(goal.id) } catch (caught) { setError(caught instanceof Error ? caught.message : 'Não foi possível excluir o objetivo.') } }; return <article className="goal-card"><h3>{goal.name}</h3><p>{money.format(goal.overallSavedAmount)} de {money.format(goal.targetAmount)} · {goal.overallProgressPercent}% · {goal.progressPercent}%</p><label>Planejado para {goal.name}<input value={planned} onChange={(event) => setPlanned(event.target.value)} /></label><label>Poupado para {goal.name}<input value={saved} onChange={(event) => setSaved(event.target.value)} /></label><button onClick={save}>Salvar mês {goal.name}</button><button aria-label={`Excluir objetivo ${goal.name}`} onClick={del}>Excluir</button>{error && <p role="alert">{error}</p>}</article> }
