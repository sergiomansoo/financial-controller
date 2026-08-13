import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { MovementFilterProvider } from '../lib/movement-filter'
import { DashboardPage } from './DashboardPage'
import { getDashboard } from '../lib/api'
vi.mock('../lib/api', () => ({ getDashboard: vi.fn() }))
it('formats commitment and investment metrics as percentages', async () => { vi.mocked(getDashboard).mockResolvedValue({ totals: { income: 1000, expense: 250, balance: 750, salaryCommittedPercent: 25.5, receivedInvestedPercent: 12 }, byCategory: [{ categoryId: 1, categoryName: 'Casa', spent: 250 }], monthlyEvolution: [{ month: '2026-08', income: 1000, expense: 250 }], budgets: [] }); render(<MemoryRouter><MovementFilterProvider><DashboardPage /></MovementFilterProvider></MemoryRouter>); expect(await screen.findByText('25,50%')).toBeInTheDocument(); expect(screen.getByText('12,00%')).toBeInTheDocument() })
