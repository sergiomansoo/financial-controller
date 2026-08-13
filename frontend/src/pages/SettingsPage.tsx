import { useMovementFilter } from '../lib/movement-filter'
export function SettingsPage() { const { filter } = useMovementFilter(); return <section className="ledger-page"><h1>Configurações</h1><p>Tema escuro ledger ativo.</p><p>Filtro persistente: {filter}</p></section> }
