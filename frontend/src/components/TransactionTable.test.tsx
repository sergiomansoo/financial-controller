import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { TransactionTable } from './TransactionTable'

const transaction = {
  id: 'transaction-1',
  date: '2026-08-12',
  description: 'Pix Store',
  amount: -45.9,
  categoryId: 'other',
  categoryName: 'Outros',
  needsReview: true,
}

const categories = [
  { id: 'other', name: 'Outros' },
  { id: 'food', name: 'Alimentação' },
]

function mockApi() {
  const fetchMock = vi.fn().mockResolvedValueOnce(
    new Response(JSON.stringify([transaction]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  ).mockResolvedValueOnce(
    new Response(JSON.stringify(categories), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  ).mockResolvedValueOnce(
    new Response(JSON.stringify({ ...transaction, categoryId: 'food', categoryName: 'Alimentação' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('TransactionTable', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('marks a duplicate candidate that needs review', async () => {
    mockApi()

    render(<TransactionTable month="2026-08" />)

    expect(await screen.findByText(/review duplicate/i)).toBeInTheDocument()
  })

  it('sends the selected category with learn enabled', async () => {
    const fetchMock = mockApi()

    render(<TransactionTable month="2026-08" />)

    const categoryEditor = await screen.findByLabelText('Category for Pix Store')
    fireEvent.change(categoryEditor, { target: { value: 'food' } })
    fireEvent.click(screen.getByRole('button', { name: 'Learn category for Pix Store' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenLastCalledWith(
        'http://localhost:8080/api/v1/transactions/transaction-1/category',
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({ categoryId: 'food', learn: true }),
        }),
      )
    })
  })
})
