import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CategoriesPage } from './CategoriesPage'
import { applyCategoryRule, createCategory, deleteCategory, getCategories, getCategoryRules } from '../lib/api'

vi.mock('../lib/api', () => ({ getCategories: vi.fn(), getCategoryRules: vi.fn(), deleteCategory: vi.fn(), deleteCategoryRule: vi.fn(), createCategory: vi.fn(), createCategoryRule: vi.fn(), applyCategoryRule: vi.fn() }))
const category = { id: 1, name: 'Alimentação' }
const rule = { id: 4, keyword: 'Mercado', category }
const ready = () => { vi.mocked(getCategories).mockResolvedValue([category]); vi.mocked(getCategoryRules).mockResolvedValue([rule]) }

describe('CategoriesPage', () => {
  beforeEach(() => vi.resetAllMocks())
  afterEach(() => vi.unstubAllGlobals())

  it('shows the API message when category deletion fails', async () => { ready(); vi.mocked(deleteCategory).mockRejectedValue(new Error('Categoria em uso')); render(<CategoriesPage />); fireEvent.click(await screen.findByRole('button', { name: /excluir alimentação/i })); expect(await screen.findByRole('alert')).toHaveTextContent('Categoria em uso') })

  it('confirms, applies a rule, and reports the changed transaction count', async () => { ready(); vi.stubGlobal('confirm', vi.fn(() => true)); vi.mocked(applyCategoryRule).mockResolvedValue({ changedCount: 2 }); render(<CategoriesPage />); fireEvent.click(await screen.findByRole('button', { name: 'Atualizar transações para Mercado' })); expect(window.confirm).toHaveBeenCalled(); expect(applyCategoryRule).toHaveBeenCalledWith(4); expect(await screen.findByRole('status')).toHaveTextContent('2 transações atualizadas.'); expect(getCategories).toHaveBeenCalledTimes(2) })

  it('keeps the rule visible and shows the Portuguese API error when applying fails', async () => { ready(); vi.stubGlobal('confirm', vi.fn(() => true)); vi.mocked(applyCategoryRule).mockRejectedValue(new Error('Não foi possível atualizar as transações.')); render(<CategoriesPage />); fireEvent.click(await screen.findByRole('button', { name: 'Atualizar transações para Mercado' })); expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível atualizar as transações.'); expect(screen.getByText('Mercado')).toBeInTheDocument() })

  it('clears an obsolete load error after successful category creation', async () => { vi.mocked(getCategories).mockRejectedValueOnce(new Error('offline')).mockResolvedValue([]); vi.mocked(getCategoryRules).mockResolvedValue([]); vi.mocked(createCategory).mockResolvedValue({ id: 2, name: 'Moradia' }); render(<CategoriesPage />); expect(await screen.findByRole('alert')).toHaveTextContent('offline'); fireEvent.change(screen.getByLabelText('Nova categoria'), { target: { value: 'Moradia' } }); fireEvent.click(screen.getByRole('button', { name: 'Criar' })); expect(await screen.findByRole('heading', { name: 'Categorias' })).toBeInTheDocument(); expect(screen.queryByRole('alert')).not.toBeInTheDocument() })
})
