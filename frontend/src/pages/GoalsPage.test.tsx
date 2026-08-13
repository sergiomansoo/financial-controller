import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { getBudgets, updateBudget } from '../lib/api'
import { GoalsPage } from './GoalsPage'
vi.mock('../lib/api', () => ({ getBudgets: vi.fn(), updateBudget: vi.fn() }))
it('updates a goal limit from its page workflow', async () => { vi.mocked(getBudgets).mockResolvedValue([{ categoryId: 3, categoryName: 'Casa', spent: 30, limit: 100, exceeded: false }]); vi.mocked(updateBudget).mockResolvedValue({ categoryId: 3, categoryName: 'Casa', spent: 30, limit: 120, exceeded: false }); render(<GoalsPage />); fireEvent.change(await screen.findByLabelText('Limite'), { target: { value: '120' } }); fireEvent.click(screen.getByRole('button', { name: 'Salvar' })); expect(await vi.mocked(updateBudget)).toHaveBeenCalledWith(3, expect.any(String), 120) })
