import { getStoredSession } from './auth'
import type {
  ApiProblem,
  AuthCredentials,
  AuthResponse,
  Category,
  Budget,
  DashboardData,
  ImportResponse,
  ManualTransactionInput,
  RegistrationDetails,
  Transaction,
  TransactionPage, TransactionQuery, ImportPreview, CategoryRule, SavingsGoal, SavingsGoalInput,
} from '../types/api'

const apiUrl = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1').replace(
  /\/$/,
  '',
)

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string | undefined,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const session = getStoredSession()
  const headers = new Headers(options.headers)

  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (session) {
    headers.set('Authorization', `Bearer ${session.token}`)
  }

  const response = await fetch(`${apiUrl}${path}`, { ...options, headers })
  const payload = (await response.json().catch(() => null)) as ApiProblem | T | null

  if (!response.ok) {
    const problem = payload as ApiProblem | null
    throw new ApiError(
      response.status,
      problem?.code,
      problem?.message ?? `Request failed with status ${response.status}`,
    )
  }

  return payload as T
}

export function login(credentials: AuthCredentials) {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
}

export function register(details: RegistrationDetails) {
  return request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(details),
  })
}

export function uploadStatement(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return request<ImportResponse>('/imports', { method: 'POST', body: formData })
}

export function getTransactions(month: string) {
  return request<Transaction[]>(`/transactions?month=${encodeURIComponent(month)}`)
}
export function getTransactionPage(query: TransactionQuery) { const params = new URLSearchParams(Object.entries(query).filter(([, value]) => value !== undefined).map(([key, value]) => [key, String(value)])); return request<TransactionPage>(`/transactions?${params}`) }
export function previewStatement(file: File) { const body = new FormData(); body.append('file', file); return request<ImportPreview>('/imports/preview', { method: 'POST', body }) }
export function createCategory(name: string) { return request<Category>('/categories', { method: 'POST', body: JSON.stringify({ name }) }) }
export function deleteCategory(id: string | number) { return request<void>(`/categories/${id}`, { method: 'DELETE' }) }
export function getCategoryRules() { return request<CategoryRule[]>('/category-rules') }
export function createCategoryRule(keyword: string, categoryId: string | number) { return request<CategoryRule>('/category-rules', { method: 'POST', body: JSON.stringify({ keyword, categoryId }) }) }
export function deleteCategoryRule(id: string | number) { return request<void>(`/category-rules/${id}`, { method: 'DELETE' }) }

export function getCategories() {
  return request<Category[]>('/categories')
}

export function updateTransactionCategory(
  id: string | number,
  categoryId: string | number,
  learn = true,
) {
  return request<Transaction>(`/transactions/${id}/category`, {
    method: 'PATCH',
    body: JSON.stringify({ categoryId, learn }),
  })
}

export function getDashboard(month: string, filter = 'both') { return request<DashboardData>(`/dashboard?month=${encodeURIComponent(month)}&filter=${filter}`) }
export function getBudgets(month: string) { return request<Budget[]>(`/budgets?month=${encodeURIComponent(month)}`) }
export function updateBudget(categoryId: string | number, month: string, limit: number): Promise<Budget> { return request<Budget>(`/budgets/${categoryId}?month=${encodeURIComponent(month)}`, { method: 'PUT', body: JSON.stringify({ limit }) }) }
export function createTransaction(transaction: ManualTransactionInput) { return request<Transaction>('/transactions', { method: 'POST', body: JSON.stringify(transaction) }) }
export function getSavingsGoals(month: string) { return request<SavingsGoal[]>(`/savings-goals?month=${encodeURIComponent(month)}`) }
export function createSavingsGoal(goal: SavingsGoalInput) { return request<SavingsGoal>('/savings-goals', { method: 'POST', body: JSON.stringify(goal) }) }
export function updateSavingsGoalMonth(id: string | number, month: string, plannedAmount: number, savedAmount: number) { return request<SavingsGoal>(`/savings-goals/${id}/months/${month}`, { method: 'PUT', body: JSON.stringify({ plannedAmount, savedAmount }) }) }
export function deleteSavingsGoal(id: string | number) { return request<void>(`/savings-goals/${id}`, { method: 'DELETE' }) }
