import { Menu, PanelLeftClose } from 'lucide-react'
import { useState, type ReactNode } from 'react'

import { MovementFilter } from './MovementFilter'
import './MonolithShell.css'
import { Sidebar } from './Sidebar'

export function AppLayout({ children }: { children: ReactNode }) {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <div className="ledger-app">
      <a className="skip-link" href="#main-content">Pular para o conteúdo</a>
      <Sidebar collapsed={collapsed} />
      <div className="ledger-content">
        <header className="ledger-header">
          <h1 className="sr-only">Financial Controller</h1>
          <button aria-label="Alternar navegação" onClick={() => setCollapsed((value) => !value)} type="button">
            {collapsed ? <Menu /> : <PanelLeftClose />}
          </button>
          <MovementFilter />
        </header>
        <main id="main-content">{children}</main>
      </div>
    </div>
  )
}
