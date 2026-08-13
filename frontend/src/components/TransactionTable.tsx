import { useEffect, useState } from 'react'

import { ApiError, getCategories, getTransactions, updateTransactionCategory } from '../lib/api'
import type { Category, Transaction } from '../types/api'
import { CategoryEditor } from './CategoryEditor'

interface TransactionTableProps {
  month: string
}

export function TransactionTable({ month }: TransactionTableProps) {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let isCurrent = true
    setIsLoading(true)
    setError(null)

    Promise.all([getTransactions(month), getCategories()])
      .then(([nextTransactions, nextCategories]) => {
        if (!isCurrent) return
        setTransactions(nextTransactions)
        setCategories(nextCategories)
      })
      .catch((caughtError) => {
        if (!isCurrent) return
        setError(
          caughtError instanceof ApiError
            ? caughtError.message
            : 'Unable to load transactions. Please try again.',
        )
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })

    return () => {
      isCurrent = false
    }
  }, [month])

  async function saveCategory(transaction: Transaction, categoryId: string) {
    const updated = await updateTransactionCategory(transaction.id, categoryId)
    setTransactions((current) => current.map((item) => item.id === transaction.id ? updated : item))
  }

  if (isLoading) return <p role="status">Loading transactions…</p>
  if (error) return <p role="alert">{error}</p>
  if (transactions.length === 0) return <p>No transactions for this month.</p>

  return (
    <section aria-labelledby="transactions-heading" className="mt-6">
      <h2 id="transactions-heading" className="text-xl font-semibold">Transactions</h2>
      <table className="mt-3 w-full border-collapse bg-white text-left">
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Description</th>
            <th scope="col">Amount</th>
            <th scope="col">Category</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((transaction) => (
            <tr key={transaction.id} className="border-t">
              <td>{transaction.date}</td>
              <td>
                {transaction.description ?? transaction.history}
                {transaction.needsReview && <span className="ml-2 rounded bg-amber-100 px-2 py-1 text-sm">Review duplicate</span>}
              </td>
              <td>{transaction.amount}</td>
              <td>
                <CategoryEditor
                  categories={categories}
                  onSave={(categoryId) => saveCategory(transaction, categoryId)}
                  transaction={transaction}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
