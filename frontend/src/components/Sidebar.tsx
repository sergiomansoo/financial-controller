import { BarChart3, Bot, FolderCog, Goal, Import, LayoutDashboard, Settings, WalletCards } from 'lucide-react'
import { useRef, useState } from 'react'
import { NavLink } from 'react-router-dom'

const items = [
  [LayoutDashboard, 'Vis\u00e3o geral', '/dashboard'],
  [WalletCards, 'Transa\u00e7\u00f5es', '/transacoes'],
  [Import, 'Importar', '/importar'],
  [FolderCog, 'Categorias', '/categorias'],
  [Goal, 'Metas', '/metas'],
  [Bot, 'Assistente IA', '/assistant'],
  [Settings, 'Configura\u00e7\u00f5es', '/configuracoes'],
] as const

interface SidebarProps {
  collapsed: boolean
  isMobileOpen?: boolean
  onNavigate?: () => void
}

export function Sidebar({ collapsed, isMobileOpen = false, onNavigate }: SidebarProps) {
  const [tooltip, setTooltip] = useState<string | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const isDesktopCollapsed = collapsed && !isMobileOpen

  const clear = () => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    setTooltip(null)
  }

  const show = (label: string) => {
    if (isDesktopCollapsed) timer.current = setTimeout(() => setTooltip(label), 300)
  }

  return (
    <nav
      aria-label={'Navega\u00e7\u00e3o principal'}
      className={`ledger-sidebar${collapsed ? ' ledger-sidebar--collapsed' : ''}${isMobileOpen ? ' ledger-sidebar--mobile-open' : ''}`}
    >
      {!collapsed && (
        <div className="ledger-brand">
          <BarChart3 aria-hidden /> <span>Ledger</span>
        </div>
      )}
      {items.map(([Icon, label, path]) => (
        <span className="nav-item" key={path}>
          <NavLink
            aria-describedby={tooltip === label ? `tooltip-${path.slice(1)}` : undefined}
            className="ledger-nav-link"
            onBlur={clear}
            onClick={onNavigate}
            onFocus={() => show(label)}
            onMouseEnter={() => show(label)}
            onMouseLeave={clear}
            to={path}
          >
            <Icon aria-hidden size={19} />
            <span>{label}</span>
          </NavLink>
          {tooltip === label && (
            <span className="sidebar-tooltip" id={`tooltip-${path.slice(1)}`} role="tooltip">
              {label}
            </span>
          )}
        </span>
      ))}
    </nav>
  )
}
