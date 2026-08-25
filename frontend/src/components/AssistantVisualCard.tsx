import type { AssistantVisualType, BudgetVisualCategory } from '../types/api'

interface Props { visualType: AssistantVisualType; visualData: unknown }
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function AssistantVisualCard({ visualType, visualData }: Props) {
  if (visualType === 'budget_summary' && isBudget(visualData)) return <section className="assistant-visual-card" aria-label="Resumo de orçamentos"><strong>Orçamentos de {visualData.month}</strong><ul>{visualData.categories.map((category) => { const percent = category.limit && category.limit > 0 ? Math.min(100, Math.round((category.spent / category.limit) * 100)) : 0; const label = category.status === 'no_limit' ? 'Sem limite definido' : category.status === 'over_limit' ? 'Acima do limite' : category.status === 'no_spending' ? 'Sem gastos' : 'Dentro do limite'; return <li key={category.name}><div><span>{category.name}</span><b>{money.format(category.spent)}</b></div><small>{category.limit ? `${money.format(category.spent)} de ${money.format(category.limit)}` : label}</small>{category.limit ? <><i aria-hidden className={`assistant-progress assistant-progress--${category.status}`} style={{ width: `${percent}%` }} /><em>{percent}% · {label}</em></> : null}</li> })}</ul></section>
  return null
}
function isBudget(value: unknown): value is { month: string; categories: BudgetVisualCategory[] } { return Boolean(value && typeof value === 'object' && Array.isArray((value as { categories?: unknown }).categories)) }
