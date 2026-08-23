import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'

export function AppLayout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  const isAuthPage = pathname === '/login' || pathname === '/register'

  return (
    <div className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <header className="mx-auto max-w-6xl">
        <h1 className="text-3xl font-bold">Financial Controller</h1>
        {!isAuthPage && (
          <nav aria-label="Navegação principal" className="mt-4 flex gap-4">
            <Link to="/dashboard">Dashboard</Link>
            <Link to="/transactions">Transações</Link>
            <Link to="/assistant">Assistente IA</Link>
          </nav>
        )}
      </header>
      {children}
    </div>
  )
}
