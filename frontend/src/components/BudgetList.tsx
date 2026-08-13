import { useEffect, useState } from 'react'
import { updateBudget } from '../lib/api'
import type { Budget } from '../types/api'

export function BudgetList({ budgets, month, onUpdated }: { budgets: Budget[]; month: string; onUpdated: () => void }) {
  const [values, setValues] = useState<Record<string, string>>(() => Object.fromEntries(budgets.map((budget) => [String(budget.categoryId), String(budget.limit)])))
  const [error, setError] = useState<string | null>(null)

  useEffect(() => setValues(Object.fromEntries(budgets.map((budget) => [String(budget.categoryId), String(budget.limit)]))), [budgets])

  async function save(budget: Budget) {
    const value = values[String(budget.categoryId)] ?? ''
    if (!/^\d+(?:\.\d{1,2})?$/.test(value) || !Number.isFinite(Number(value)) || Number(value) < 0) {
      setError('Enter a non-negative limit with at most two decimal places.')
      return
    }
    try { setError(null); await updateBudget(budget.categoryId, month, Number(value)); onUpdated() } catch { setError('Unable to update budget. Please try again.') }
  }

  if (!budgets.length) return <p>No budgets for this month.</p>
  return <section aria-labelledby="budgets-heading"><h2 id="budgets-heading">Budgets</h2>{error && <p role="alert">{error}</p>}<ul>{budgets.map((budget) => <li key={budget.categoryId}><strong>{budget.categoryName}</strong>{budget.exceeded && <span role="alert">{budget.categoryName} budget exceeded</span>}<label htmlFor={`budget-${budget.categoryId}`}>Limit for {budget.categoryName}</label><input id={`budget-${budget.categoryId}`} min="0" onChange={(event) => setValues((current) => ({ ...current, [String(budget.categoryId)]: event.target.value }))} step="0.01" type="number" value={values[String(budget.categoryId)] ?? ''} /><button onClick={() => void save(budget)} type="button">Save budget</button></li>)}</ul></section>
}
