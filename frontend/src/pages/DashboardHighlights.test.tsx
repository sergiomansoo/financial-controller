import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { DashboardPage } from './DashboardPage'
import { MovementFilterProvider } from '../lib/movement-filter'
import { getDashboard, getSavingsGoals, getTransactionTotal } from '../lib/api'

vi.mock('../lib/api', () => ({ getDashboard: vi.fn(), getSavingsGoals: vi.fn(), getTransactionTotal: vi.fn() }))

describe('Dashboard summary layout', () => {
  it('does not render the redundant largest income and expense strip', async () => {
    vi.mocked(getSavingsGoals).mockResolvedValue([])
    vi.mocked(getTransactionTotal).mockResolvedValue({ total: 0, totalSpent: 0 })
    vi.mocked(getDashboard).mockResolvedValue({
      totals: {
        income: 759.31,
        expense: 575,
        balance: 184.31,
        largestExpense: { categoryId: 1, categoryName: 'Faculdade', amount: 575 },
        largestIncome: { categoryId: 2, categoryName: 'Outros', amount: 759.31 },
      },
      byCategory: [],
      monthlyEvolution: [{ month: '2025-11', income: 759.31, expense: 575 }],
      budgets: [],
    })
    render(<MemoryRouter><MovementFilterProvider><DashboardPage /></MovementFilterProvider></MemoryRouter>)
    expect(await screen.findByTestId('monolith-kpi-grid')).toBeInTheDocument()
    expect(screen.queryByText('Maior despesa')).not.toBeInTheDocument()
    expect(screen.queryByText('Maior receita')).not.toBeInTheDocument()
  })
})
