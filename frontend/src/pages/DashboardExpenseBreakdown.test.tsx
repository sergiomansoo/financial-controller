import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { DashboardPage } from './DashboardPage'
import { MovementFilterProvider } from '../lib/movement-filter'
import { getDashboard, getSavingsGoals, getTransactionTotal } from '../lib/api'

vi.mock('../lib/api', () => ({ getDashboard: vi.fn(), getSavingsGoals: vi.fn(), getTransactionTotal: vi.fn() }))

describe('Dashboard expense breakdown', () => {
  it('uses expense-only category data instead of filtered mixed categories', async () => {
    vi.mocked(getSavingsGoals).mockResolvedValue([])
    vi.mocked(getTransactionTotal).mockResolvedValue({ total: 0, totalSpent: 0 })
    vi.mocked(getDashboard).mockResolvedValue({
      totals: { income: 1000, expense: 120, balance: 880 },
      byCategory: [{ categoryId: 'income', categoryName: 'Salário', spent: 1000 }],
      expenseByCategory: [{ categoryId: 'expense', categoryName: 'Despesa exclusiva', spent: 120 }],
      monthlyEvolution: [{ month: '2026-08', income: 1000, expense: 120 }],
      budgets: [],
    })
    render(<MemoryRouter><MovementFilterProvider><DashboardPage /></MovementFilterProvider></MemoryRouter>)
    expect(await screen.findByText('Despesa exclusiva')).toBeInTheDocument()
    expect(screen.queryByText('Salário')).not.toBeInTheDocument()
  })
})
