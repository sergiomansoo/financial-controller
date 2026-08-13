import { getStoredSession } from './auth'
import type {
  ApiProblem,
  AuthCredentials,
  AuthResponse,
  RegistrationDetails,
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

  headers.set('Content-Type', 'application/json')
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
