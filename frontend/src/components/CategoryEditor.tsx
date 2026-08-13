import { useState } from 'react'

import type { Category, Transaction } from '../types/api'

interface CategoryEditorProps {
  categories: Category[]
  transaction: Transaction
  onSave: (categoryId: string) => Promise<void>
}

export function CategoryEditor({ categories, transaction, onSave }: CategoryEditorProps) {
  const [categoryId, setCategoryId] = useState(String(transaction.categoryId))
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function save() {
    setError(null)
    setIsSaving(true)
    try {
      await onSave(categoryId)
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : 'Unable to update the category.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div>
      <label htmlFor={`category-${transaction.id}`}>Category for {transaction.description}</label>
      <select
        className="ml-2 border p-1"
        id={`category-${transaction.id}`}
        onChange={(event) => setCategoryId(event.target.value)}
        value={categoryId}
      >
        {categories.map((category) => (
          <option key={category.id} value={String(category.id)}>{category.name}</option>
        ))}
      </select>
      <button
        className="ml-2 rounded border px-2 py-1"
        disabled={isSaving}
        onClick={save}
        type="button"
      >
        {isSaving ? 'Saving…' : `Learn category for ${transaction.description}`}
      </button>
      {error && <p role="alert">{error}</p>}
    </div>
  )
}
