import type { ReactNode } from 'react'

export function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <header>
        <h1 className="text-3xl font-bold">Financial Controller</h1>
      </header>
      {children}
    </div>
  )
}
