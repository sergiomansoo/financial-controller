import { BarChart3, FolderCog, Goal, Import, LayoutDashboard, Settings, WalletCards } from 'lucide-react'
import { NavLink } from 'react-router-dom'

const items = [[LayoutDashboard, 'Visão geral', '/dashboard'], [WalletCards, 'Transações', '/transacoes'], [Import, 'Importar', '/importar'], [FolderCog, 'Categorias', '/categorias'], [Goal, 'Metas', '/metas'], [Settings, 'Configurações', '/configuracoes']] as const
export function Sidebar({ collapsed }: { collapsed: boolean }) { return <nav aria-label="Navegação principal" className={`ledger-sidebar ${collapsed ? 'ledger-sidebar--collapsed' : ''}`}>{!collapsed && <div className="ledger-brand"><BarChart3 aria-hidden /> <span>Ledger</span></div>}{items.map(([Icon, label, path]) => <NavLink className="ledger-nav-link" key={path} to={path}><Icon aria-hidden size={19} /><span>{label}</span></NavLink>)}</nav> }
