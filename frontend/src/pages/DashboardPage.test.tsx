import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getDashboard } from '../lib/api'
import { MovementFilterProvider } from '../lib/movement-filter'
import { DashboardPage } from './DashboardPage'
vi.mock('../lib/api', () => ({ getDashboard: vi.fn() }))
const getDashboardMock = vi.mocked(getDashboard)
const dashboard = (month: string) => ({ totals: { income: 100, expense: 10, balance: 90 }, byCategory: [{ categoryId: 'food', categoryName: `Food ${month}`, spent: 10 }], monthlyEvolution: [{ month, income: 100, expense: 10 }], budgets: [] })
const renderPage = () => render(<MovementFilterProvider><DashboardPage /></MovementFilterProvider>)
describe('DashboardPage', () => { afterEach(() => vi.clearAllMocks()); it('shows loading then an error when dashboard loading fails', async () => { getDashboardMock.mockRejectedValueOnce(new Error('offline')); renderPage(); expect(screen.getByRole('status')).toHaveTextContent('Carregando painel'); expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível carregar o painel.') }); it('ignores an obsolete month response after a fast month change', async () => { let resolveOld!: (value: ReturnType<typeof dashboard>) => void; const oldRequest = new Promise<ReturnType<typeof dashboard>>((resolve) => { resolveOld = resolve }); getDashboardMock.mockReturnValueOnce(oldRequest).mockResolvedValueOnce(dashboard('2026-09')); renderPage(); fireEvent.change(screen.getByLabelText('Mês'), { target: { value: '2026-09' } }); expect((await screen.findAllByText('Food 2026-09')).length).toBeGreaterThan(0); await act(async () => resolveOld(dashboard('old'))); expect(screen.queryByText('Food old')).not.toBeInTheDocument() }) })
