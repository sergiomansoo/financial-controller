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
export interface DashboardHighlight { categoryId: string | number; categoryName: string; amount: number }
export interface DashboardTotals { income: number; expense: number; balance: number; largestIncome?: DashboardHighlight; largestExpense?: DashboardHighlight; largestExpenseCategory?: string; largestExpenseAmount?: number; salaryCommittedPercent?: number; receivedInvestedPercent?: number }
export interface DashboardData { totals?: DashboardTotals; byCategory: CategorySpending[]; monthlyEvolution: MonthlyEvolution[]; budgets: Budget[] }
export interface TransactionPage { content: Transaction[]; page: number; size: number; totalElements: number; totalPages: number }
export interface TransactionQuery { month?: string; type?: TransactionType; categoryId?: string; from?: string; to?: string; page?: number; size?: number }
export interface TransactionTotal { total: number; totalSpent: number }
export interface TransactionTotalQuery { month?: string; from?: string; to?: string; type?: TransactionType; categoryId?: string }
export interface ImportPreviewRow { date: string; history: string; description: string | null; amount: number; type: string; duplicate: boolean }
export interface ImportPreview { rows: ImportPreviewRow[]; previewCount: number; duplicateCount: number }
export interface CategoryRule { id: string | number; keyword: string; category: Category }
export interface CategoryRuleApplyResponse { changedCount: number }
export type TransactionType = 'EXPENSE' | 'INCOME' | 'INVESTMENT'
export interface ManualTransactionInput { date: string; description: string; amount: number; categoryId: string | number; type: TransactionType }
export interface SavingsGoal { id: string | number; name: string; targetAmount: number; targetDate: string | null; month: string; plannedAmount: number; savedAmount: number; progressPercent: number; overallSavedAmount: number; overallProgressPercent: number }
export interface SavingsGoalInput { name: string; targetAmount: number; targetDate: string | null }
