import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getDashboard } from '../lib/api'
import { DashboardPage } from './DashboardPage'

vi.mock('../lib/api', () => ({ getDashboard: vi.fn() }))
vi.mock('../components/CategoryPieChart', () => ({ CategoryPieChart: ({ data }: { data: { categoryName: string }[] }) => <p>{data[0]?.categoryName}</p> }))
vi.mock('../components/MonthlyChart', () => ({ MonthlyChart: () => <p>Monthly chart</p> }))
vi.mock('../components/BudgetList', () => ({ BudgetList: ({ onUpdated }: { onUpdated: () => void }) => <button onClick={onUpdated}>Refresh budget</button> }))
vi.mock('../components/ManualTransactionForm', () => ({ ManualTransactionForm: ({ onCreated }: { onCreated: () => void }) => <button onClick={onCreated}>Refresh transaction</button> }))

const getDashboardMock = vi.mocked(getDashboard)
const dashboard = (month: string) => ({ byCategory: [{ categoryId: 'food', categoryName: `Food ${month}`, spent: 10 }], monthlyEvolution: [{ month, income: 100, expense: 10 }], budgets: [] })

describe('DashboardPage', () => {
  afterEach(() => vi.clearAllMocks())

  it('shows loading then an error when dashboard loading fails', async () => {
    getDashboardMock.mockRejectedValueOnce(new Error('offline'))
    render(<DashboardPage />)
    expect(screen.getByRole('status')).toHaveTextContent('Loading dashboard')
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load dashboard. Please try again.')
  })

  it('ignores an obsolete month response after a fast month change', async () => {
    let resolveOld!: (value: ReturnType<typeof dashboard>) => void
    const oldRequest = new Promise<ReturnType<typeof dashboard>>((resolve) => { resolveOld = resolve })
    getDashboardMock.mockReturnValueOnce(oldRequest).mockResolvedValueOnce(dashboard('2026-09'))
    render(<DashboardPage />)
    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-09' } })
    expect(await screen.findByText('Food 2026-09')).toBeInTheDocument()
    await act(async () => resolveOld(dashboard('old')))
    expect(screen.queryByText('Food old')).not.toBeInTheDocument()
  })

  it.each(['Refresh budget', 'Refresh transaction'])('reloads dashboard data after %s', async (buttonName) => {
    getDashboardMock.mockResolvedValueOnce(dashboard('2026-08')).mockResolvedValueOnce(dashboard('2026-08 refreshed'))
    render(<DashboardPage />)
    expect(await screen.findByText('Food 2026-08')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: buttonName }))
    await waitFor(() => expect(getDashboardMock).toHaveBeenCalledTimes(2))
  })
})
