import { getStoredSession } from './auth'
import type {
  ApiProblem,
  AssistantChatRequest,
  AssistantChatResponse,
  AuthCredentials,
  AuthResponse,
  Category,
  Budget,
  DashboardData,
  ImportResponse,
  ManualTransactionInput,
  RegistrationDetails,
  Transaction,
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

export function getCategories() {
  return request<Category[]>('/categories')
}

export function updateTransactionCategory(
  id: string | number,
  categoryId: string | number,
) {
  return request<Transaction>(`/transactions/${id}/category`, {
    method: 'PATCH',
    body: JSON.stringify({ categoryId, learn: true }),
  })
}

export function getDashboard(month: string) { return request<DashboardData>(`/dashboard?month=${encodeURIComponent(month)}`) }
export function updateBudget(categoryId: string | number, month: string, limit: number): Promise<Budget> { return request<Budget>(`/budgets/${categoryId}?month=${encodeURIComponent(month)}`, { method: 'PUT', body: JSON.stringify({ limit }) }) }
export function createTransaction(transaction: ManualTransactionInput) { return request<Transaction>('/transactions', { method: 'POST', body: JSON.stringify(transaction) }) }

export function askAssistant(input: AssistantChatRequest) {
  return request<AssistantChatResponse>('/assistant/chat', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}
