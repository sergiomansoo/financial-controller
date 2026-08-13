import { useEffect, useState } from 'react'
import { BudgetList } from '../components/BudgetList'
import { CategoryPieChart } from '../components/CategoryPieChart'
import { ManualTransactionForm } from '../components/ManualTransactionForm'
import { MonthlyChart } from '../components/MonthlyChart'
import { getDashboard } from '../lib/api'
import type { DashboardData } from '../types/api'
const currentMonth = () => new Date().toISOString().slice(0, 7)
export function DashboardPage() { const [month, setMonth] = useState(currentMonth); const [data, setData] = useState<DashboardData | null>(null); const [error, setError] = useState<string | null>(null); const [refresh, setRefresh] = useState(0); useEffect(() => { setData(null); setError(null); getDashboard(month).then(setData).catch(() => setError('Unable to load dashboard. Please try again.')) }, [month, refresh]); return <main className="mx-auto max-w-6xl space-y-6 p-6"><h1 className="text-3xl font-bold">Dashboard</h1><label htmlFor="dashboard-month">Month</label><input id="dashboard-month" onChange={(event) => setMonth(event.target.value)} type="month" value={month} />{error && <p role="alert">{error}</p>}{!data && !error && <p role="status">Loading dashboard…</p>}{data && <><div className="grid gap-6 md:grid-cols-2"><CategoryPieChart data={data.byCategory} /><MonthlyChart data={data.monthlyEvolution} /></div><div className="grid gap-6 md:grid-cols-2"><BudgetList budgets={data.budgets} month={month} onUpdated={() => setRefresh((n) => n + 1)} /><ManualTransactionForm onCreated={() => setRefresh((n) => n + 1)} /></div></>}</main> }
