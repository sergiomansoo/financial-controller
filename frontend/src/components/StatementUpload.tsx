import { type FormEvent, useState } from 'react'

import { ApiError, uploadStatement } from '../lib/api'
import type { ImportResponse } from '../types/api'

interface StatementUploadProps {
  onImported?: (result: ImportResponse) => void
}

export function StatementUpload({ onImported }: StatementUploadProps) {
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<ImportResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) return

    setError(null)
    setResult(null)
    setIsSubmitting(true)

    try {
      const imported = await uploadStatement(file)
      setResult(imported)
      onImported?.(imported)
    } catch (caughtError) {
      setError(
        caughtError instanceof ApiError
          ? caughtError.message
          : 'Unable to upload the statement. Please try again.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="statement-upload-heading" className="mt-6 rounded border bg-white p-4">
      <h2 id="statement-upload-heading" className="text-xl font-semibold">Import statement</h2>
      <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="statement-file">Statement file</label>
          <input
            accept=".csv,text/csv"
            className="ml-2"
            id="statement-file"
            name="file"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            required
            type="file"
          />
        </div>
        {error && <p role="alert">{error}</p>}
        {result && (
          <p role="status">
            Imported {result.importedCount} transactions; {result.duplicateCount} duplicates need review.
          </p>
        )}
        <button className="rounded bg-slate-900 px-4 py-2 text-white" disabled={isSubmitting} type="submit">
          {isSubmitting ? 'Uploading…' : 'Upload statement'}
        </button>
      </form>
    </section>
  )
}
