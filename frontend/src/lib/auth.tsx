import { createContext, useContext, useState, type ReactNode } from 'react'

import type { User } from '../types/api'

const SESSION_KEY = 'financial-controller.session'

export interface Session {
  token: string
  user: User
}

interface AuthContextValue {
  session: Session | null
  saveSession: (session: Session) => void
  clearSession: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function getStoredSession(): Session | null {
  const storedSession = localStorage.getItem(SESSION_KEY)

  if (!storedSession) {
    return null
  }

  try {
    return JSON.parse(storedSession) as Session
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(getStoredSession)

  const saveSession = (nextSession: Session) => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession))
    setSession(nextSession)
  }

  const clearSession = () => {
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
  }

  return (
    <AuthContext.Provider value={{ session, saveSession, clearSession }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }

  return context
}
