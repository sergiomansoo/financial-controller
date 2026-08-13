import { useState } from 'react'
import { updateBudget } from '../lib/api'
import type { Budget } from '../types/api'

export function BudgetList({ budgets, month, onUpdated }: { budgets: Budget[]; month: string; onUpdated: () => void }) {
  const [error, setError] = useState<string | null>(null)
  async function save(categoryId: string | number, value: string) { try { setError(null); await updateBudget(categoryId, month, Number(value)); onUpdated() } catch { setError('Unable to update budget. Please try again.') } }
  if (!budgets.length) return <p>No budgets for this month.</p>
  return <section aria-labelledby="budgets-heading" className="rounded border bg-white p-4"><h2 id="budgets-heading" className="text-xl font-semibold">Budgets</h2>{error && <p role="alert">{error}</p>}<ul className="mt-3 space-y-3">{budgets.map((budget) => <li className={budget.exceeded ? 'rounded border border-red-500 p-3' : 'rounded border p-3'} key={budget.categoryId}><strong>{budget.categoryName}</strong>{budget.exceeded && <span className="ml-2" role="alert">{budget.categoryName} budget exceeded</span>}<p>Spent {budget.spent} of {budget.limit}</p><label htmlFor={`budget-${budget.categoryId}`}>Limit for {budget.categoryName}</label><input defaultValue={budget.limit} id={`budget-${budget.categoryId}`} min="0" step="0.01" type="number" /><button className="ml-2 rounded border px-2 py-1" onClick={() => { const input = document.getElementById(`budget-${budget.categoryId}`) as HTMLInputElement; void save(budget.categoryId, input.value) }} type="button">Save budget</button></li>)}</ul></section>
}
