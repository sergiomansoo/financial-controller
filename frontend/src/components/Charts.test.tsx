import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CategoryPieChart } from './CategoryPieChart'
import { MonthlyChart } from './MonthlyChart'

describe('dashboard charts', () => {
  it('provides a textual category data table with a caption and value', () => {
    render(<CategoryPieChart data={[{ categoryId: 'food', categoryName: 'Food', spent: 45.9 }]} />)
    expect(screen.getByRole('table', { name: 'Category spending data' })).toHaveTextContent('Food')
    expect(screen.getByRole('table', { name: 'Category spending data' })).toHaveTextContent('45.9')
  })
  it('provides a textual monthly data table with a caption and values', () => {
    render(<MonthlyChart data={[{ month: '2026-08', income: 1000, expense: 45.9 }]} />)
    expect(screen.getByRole('table', { name: 'Monthly income and expense data' })).toHaveTextContent('2026-08')
    expect(screen.getByRole('table', { name: 'Monthly income and expense data' })).toHaveTextContent('1000')
  })
})
