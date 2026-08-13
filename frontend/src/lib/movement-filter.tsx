import { createContext, useContext, useState, type ReactNode } from 'react'

export type MovementFilter = 'both' | 'income' | 'expense'
const key = 'financial-controller.movement-filter'
const Context = createContext<{ filter: MovementFilter; setFilter: (filter: MovementFilter) => void } | null>(null)
function read(): MovementFilter { const value = localStorage.getItem(key); return value === 'income' || value === 'expense' ? value : 'both' }
export function MovementFilterProvider({ children }: { children: ReactNode }) { const [filter, setValue] = useState<MovementFilter>(read); const setFilter = (next: MovementFilter) => { localStorage.setItem(key, next); setValue(next) }; return <Context.Provider value={{ filter, setFilter }}>{children}</Context.Provider> }
export function useMovementFilter() { const context = useContext(Context); if (!context) throw new Error('useMovementFilter must be used within MovementFilterProvider'); return context }
