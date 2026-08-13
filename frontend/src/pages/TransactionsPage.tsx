import { useState } from 'react'

import { StatementUpload } from '../components/StatementUpload'
import { TransactionTable } from '../components/TransactionTable'

function currentMonth() {
  return new Date().toISOString().slice(0, 7)
}

export function TransactionsPage() {
  const [month, setMonth] = useState(currentMonth)
  const [refreshKey, setRefreshKey] = useState(0)

  return (
    <main className="mx-auto max-w-5xl p-6">
      <h1 className="text-3xl font-bold">Review transactions</h1>
      <StatementUpload onImported={() => setRefreshKey((current) => current + 1)} />
      <div className="mt-6">
        <label htmlFor="transactions-month">Month</label>
        <input
          className="ml-2 border p-1"
          id="transactions-month"
          onChange={(event) => setMonth(event.target.value)}
          type="month"
          value={month}
        />
      </div>
      <TransactionTable key={`${month}-${refreshKey}`} month={month} />
    </main>
  )
}
