import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'

import { useAuth } from '../lib/auth'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { session } = useAuth()

  return session ? <>{children}</> : <Navigate to="/login" replace />
}
