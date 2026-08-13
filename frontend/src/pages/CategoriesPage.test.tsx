import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { CategoriesPage } from './CategoriesPage'
import { deleteCategory, getCategories, getCategoryRules } from '../lib/api'
vi.mock('../lib/api', () => ({ getCategories: vi.fn(), getCategoryRules: vi.fn(), deleteCategory: vi.fn(), deleteCategoryRule: vi.fn(), createCategory: vi.fn(), createCategoryRule: vi.fn() }))
it('shows the API message when category deletion fails', async () => { vi.mocked(getCategories).mockResolvedValue([{ id: 1, name: 'Alimentação' }]); vi.mocked(getCategoryRules).mockResolvedValue([]); vi.mocked(deleteCategory).mockRejectedValue(new Error('Categoria em uso')); render(<CategoriesPage />); fireEvent.click(await screen.findByRole('button', { name: /excluir alimentação/i })); expect(await screen.findByRole('alert')).toHaveTextContent('Categoria em uso') })
