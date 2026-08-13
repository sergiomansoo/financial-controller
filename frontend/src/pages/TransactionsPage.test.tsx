import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MovementFilterProvider } from '../lib/movement-filter'
import { getCategories, getTransactionPage } from '../lib/api'
import { TransactionsPage } from './TransactionsPage'
vi.mock('../lib/api', () => ({ getCategories: vi.fn(), getTransactionPage: vi.fn() }))
describe('TransactionsPage', () => { it('shows a category badge and actions column without removing pagination', async () => { vi.mocked(getCategories).mockResolvedValue([{ id: 1, name: 'Alimentação' }]); vi.mocked(getTransactionPage).mockResolvedValue({ content: [{ id: 7, date: '2026-08-02', history: 'Mercado', description: null, amount: 25, category: { id: 1, name: 'Alimentação' }, type: 'EXPENSE', needsReview: false }], page: 0, size: 10, totalElements: 12, totalPages: 2 }); render(<MovementFilterProvider><TransactionsPage /></MovementFilterProvider>); expect(await screen.findByText('Alimentação', { selector: 'span.category-badge' })).toHaveClass('category-badge'); expect(screen.getByRole('columnheader', { name: 'Ações' })).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Ver transação Mercado' })).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Página 2' })).toBeInTheDocument() }) })
