export interface User {
  id: string | number
  name: string
  email: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  user: User
}

export interface AuthCredentials {
  email: string
  password: string
}

export interface RegistrationDetails extends AuthCredentials {
  name: string
}

export interface ApiProblem {
  code?: string
  message?: string
}

export interface Category {
  id: string | number
  name: string
}

export interface ImportResponse {
  importedCount: number
  duplicateCount: number
  transactions: Transaction[]
}

export interface Transaction {
  id: string | number
  date: string
  history: string
  description: string | null
  amount: number
  category: Category
  type: string
  needsReview: boolean
}

export interface Budget { categoryId: string | number; categoryName: string; limit: number; spent: number; exceeded: boolean }
export interface CategorySpending { categoryId: string | number; categoryName: string; spent: number }
export interface MonthlyEvolution { month: string; income: number; expense: number }
export interface DashboardData { byCategory: CategorySpending[]; monthlyEvolution: MonthlyEvolution[]; budgets: Budget[] }
export type TransactionType = 'EXPENSE' | 'INCOME' | 'INVESTMENT'
export interface ManualTransactionInput { date: string; description: string; amount: number; categoryId: string | number; type: TransactionType }

export type AssistantRole = 'user' | 'assistant'
export interface AssistantHistoryMessage { role: AssistantRole; content: string }
export interface AssistantChatRequest { message: string; month: string; history: AssistantHistoryMessage[] }
export interface AssistantChatResponse { message: string }
