import { Menu, PanelLeftClose } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'

import { MovementFilter } from './MovementFilter'
import './MonolithShell.css'
import { Sidebar } from './Sidebar'

const phoneNavigationQuery = '(max-width: 767px)'

export function AppLayout({ children }: { children: ReactNode }) {
  const [collapsed, setCollapsed] = useState(false)
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false)
  const menuButtonRef = useRef<HTMLButtonElement>(null)
  const isPhone =
    typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(phoneNavigationQuery).matches

  const closeMobileNavigation = (restoreFocus = false) => {
    setIsMobileNavOpen(false)

    if (restoreFocus) {
      menuButtonRef.current?.focus()
    }
  }

  useEffect(() => {
    if (!isMobileNavOpen) {
      return
    }

    const closeWithEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeMobileNavigation(true)
      }
    }

    document.body.classList.add('ledger-nav-open')
    window.addEventListener('keydown', closeWithEscape)

    return () => {
      document.body.classList.remove('ledger-nav-open')
      window.removeEventListener('keydown', closeWithEscape)
    }
  }, [isMobileNavOpen])

  const toggleNavigation = () => {
    if (isPhone) {
      setIsMobileNavOpen((value) => !value)
      return
    }

    setCollapsed((value) => !value)
  }

  const menuLabel = isPhone
    ? isMobileNavOpen
      ? 'Fechar navega\u00e7\u00e3o'
      : 'Abrir navega\u00e7\u00e3o'
    : 'Alternar navega\u00e7\u00e3o'

  return (
    <div className="ledger-app">
      <a className="skip-link" href="#main-content">
        {'Pular para o conte\u00fado'}
      </a>
      {isPhone && isMobileNavOpen && (
        <button aria-label="Fechar menu" className="ledger-nav-backdrop" onClick={() => closeMobileNavigation(true)} type="button" />
      )}
      <Sidebar collapsed={isPhone ? false : collapsed} isMobileOpen={isPhone && isMobileNavOpen} onNavigate={closeMobileNavigation} />
      <div className="ledger-content">
        <header className="ledger-header">
          <h1 className="sr-only">Financial Controller</h1>
          <button
            aria-expanded={isPhone ? isMobileNavOpen : undefined}
            aria-label={menuLabel}
            onClick={toggleNavigation}
            ref={menuButtonRef}
            type="button"
          >
            {isPhone ? isMobileNavOpen ? <PanelLeftClose /> : <Menu /> : collapsed ? <Menu /> : <PanelLeftClose />}
          </button>
          <MovementFilter />
        </header>
        <main id="main-content">{children}</main>
      </div>
    </div>
  )
}
