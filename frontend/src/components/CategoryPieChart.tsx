import { Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import type { CategorySpending } from '../types/api'
export function CategoryPieChart({ data }: { data: CategorySpending[] }) { if (!data.length) return <p>No category spending for this month.</p>; return <section aria-labelledby="category-chart-heading" className="h-72 rounded border bg-white p-4"><h2 id="category-chart-heading">Spending by category</h2><ResponsiveContainer><PieChart><Pie data={data} dataKey="spent" nameKey="categoryName" label /><Tooltip /></PieChart></ResponsiveContainer></section> }
