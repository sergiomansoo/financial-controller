import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { LedgerCharts } from './LedgerCharts'
vi.mock('recharts', () => ({ ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>, BarChart: ({ children }: { children: ReactNode }) => <div>{children}</div>, LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>, Bar: () => null, Line: () => null, CartesianGrid: () => null, Legend: () => null, Tooltip: () => null, XAxis: () => null, YAxis: () => null }))
describe('LedgerCharts', () => { it('renders localized category and six-month evolution chart labels', () => { render(<LedgerCharts categories={[{ categoryId: 1, categoryName: 'Mercado', spent: 120 }]} evolution={[{ month: '2026-08', income: 1000, expense: 120 }]} />); expect(screen.getByRole('heading', { name: 'Gastos por categoria' })).toBeInTheDocument(); expect(screen.getByRole('heading', { name: 'Evolução dos últimos seis meses' })).toBeInTheDocument(); expect(screen.getByLabelText('Gráfico de barras de gastos por categoria')).toBeInTheDocument() }) })
