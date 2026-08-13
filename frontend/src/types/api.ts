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
  description: string
  amount: number
  categoryId: string | number
  categoryName: string
  needsReview: boolean
}
