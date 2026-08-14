import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { getDashboard, getSavingsGoals, getTransactionTotal } from '../lib/api'
import { MovementFilterProvider } from '../lib/movement-filter'
import { DashboardPage } from './DashboardPage'

vi.mock('../lib/api', () => ({ getDashboard: vi.fn(), getSavingsGoals: vi.fn(), getTransactionTotal: vi.fn() }))
const getDashboardMock = vi.mocked(getDashboard)
const dashboard = (month: string) => ({ totals: { income: 100, expense: 10, balance: 90 }, byCategory: [{ categoryId: 'food', categoryName: `Food ${month}`, spent: 10 }], monthlyEvolution: [{ month, income: 100, expense: 10 }], budgets: [] })
const renderPage = () => render(<MemoryRouter><MovementFilterProvider><DashboardPage /></MovementFilterProvider></MemoryRouter>)

describe('DashboardPage', () => {
  beforeEach(() => { localStorage.clear(); vi.mocked(getSavingsGoals).mockResolvedValue([]); vi.mocked(getTransactionTotal).mockResolvedValue({ total: 0, totalSpent: 0 }) })
  afterEach(() => vi.clearAllMocks())

  it('shows loading then an error when dashboard loading fails', async () => {
    getDashboardMock.mockRejectedValueOnce(new Error('offline'))
    renderPage()
    expect(screen.getByRole('status')).toHaveTextContent('Carregando painel')
    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })
  it('ignores an obsolete month response after a fast month change', async () => {
    let resolveOld!: (value: ReturnType<typeof dashboard>) => void
    const oldRequest = new Promise<ReturnType<typeof dashboard>>((resolve) => { resolveOld = resolve })
    getDashboardMock.mockReturnValueOnce(oldRequest).mockResolvedValueOnce(dashboard('2026-09'))
    renderPage()
    fireEvent.change(screen.getByLabelText('Mês'), { target: { value: '2026-09' } })
    expect((await screen.findAllByText('Food 2026-09')).length).toBeGreaterThan(0)
    await act(async () => resolveOld(dashboard('old')))
    expect(screen.queryByText('Food old')).not.toBeInTheDocument()
  })
  it('offers statement import when every monthly value is zero', async () => {
    getDashboardMock.mockResolvedValueOnce({ totals: { income: 0, expense: 0, balance: 0 }, byCategory: [], monthlyEvolution: [{ month: '2026-03', income: 0, expense: 0 }], budgets: [] })
    renderPage()
    expect(await screen.findByRole('link', { name: 'Importar extrato' })).toHaveAttribute('href', '/importar')
  })
  it('renders four aligned financial KPI cards and no redundant highlights strip', async () => {
    getDashboardMock.mockResolvedValue(dashboard('2026-08'))
    renderPage()
    expect(await screen.findByRole('region', { name: 'Painel financeiro' })).toBeInTheDocument()
    const kpis = screen.getByTestId('monolith-kpi-grid')
    expect(kpis.querySelectorAll('.monolith-kpi')).toHaveLength(4)
    expect(screen.queryByText('Maior despesa')).not.toBeInTheDocument()
    expect(screen.queryByText('Maior receita')).not.toBeInTheDocument()
  })
  it('counts each savings goal once by id using its overall saved amount', async () => {
    getDashboardMock.mockResolvedValue(dashboard('2026-08'))
    vi.mocked(getSavingsGoals).mockResolvedValue([{ id: 1, name: 'Reserva', targetAmount: 1000, overallSavedAmount: 125, overallProgressPercent: 12.5 }, { id: 1, name: 'Reserva', targetAmount: 1000, overallSavedAmount: 125, overallProgressPercent: 12.5 }] as never)
    renderPage()
    expect(await screen.findByTestId('monolith-kpi-grid')).toHaveTextContent(/125,00/)
  })
})
