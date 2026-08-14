import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { LedgerCharts } from './LedgerCharts'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  PieChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  Pie: () => null,
  Cell: () => null,
  Line: () => null,
  CartesianGrid: () => null,
  Tooltip: ({ contentStyle }: { contentStyle?: { backgroundColor?: string; border?: string } }) => <output data-testid="monolith-chart-tooltip" data-background={contentStyle?.backgroundColor} data-border={contentStyle?.border} />,
  XAxis: () => null,
  YAxis: () => null,
}))

const categories = [{ categoryId: 1, categoryName: 'Mercado', spent: 120 }]
const evolution = [{ month: '2026-08', income: 1000, expense: 120 }]

describe('LedgerCharts', () => {
  it('renders the expense breakdown and six-month evolution chart labels', () => {
    render(<LedgerCharts categories={categories} evolution={evolution} />)
    expect(screen.getByRole('heading', { name: 'Detalhamento de Despesas' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Evolução dos últimos seis meses' })).toBeInTheDocument()
  })

  it('can render the line chart and expense donut in separate dashboard cards', () => {
    const { rerender } = render(<LedgerCharts categories={categories} evolution={evolution} showCategories={false} />)
    expect(screen.queryByRole('heading', { name: 'Detalhamento de Despesas' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Evolução dos últimos seis meses' })).toBeInTheDocument()
    rerender(<LedgerCharts categories={categories} evolution={evolution} showEvolution={false} />)
    expect(screen.getByRole('heading', { name: 'Detalhamento de Despesas' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Evolução dos últimos seis meses' })).not.toBeInTheDocument()
  })

  it('renders a grayscale expense donut with central total and only the top three categories', () => {
    render(<LedgerCharts categories={[
      { categoryId: 1, categoryName: 'Moradia', spent: 300 },
      { categoryId: 2, categoryName: 'Mercado', spent: 200 },
      { categoryId: 3, categoryName: 'Transporte', spent: 100 },
      { categoryId: 4, categoryName: 'Outros', spent: 50 },
    ]} evolution={[]} showEvolution={false} />)
    expect(screen.getByText('Despesa Total')).toBeInTheDocument()
    expect(screen.getByText('R$ 650,00')).toBeInTheDocument()
    const list = screen.getByRole('list', { name: 'Três maiores despesas por categoria' })
    expect(list).toHaveTextContent('Moradia')
    expect(list).toHaveTextContent('Transporte')
    expect(list).not.toHaveTextContent('Outros')
  })

  it('styles chart tooltips for the Monolith surface', () => {
    render(<LedgerCharts categories={categories} evolution={evolution} />)
    expect(screen.getAllByTestId('monolith-chart-tooltip')).toHaveLength(2)
    expect(screen.getAllByTestId('monolith-chart-tooltip')[0]).toHaveAttribute('data-background', '#18181a')
  })
})
