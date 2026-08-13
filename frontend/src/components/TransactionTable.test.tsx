import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { TransactionTable } from './TransactionTable'

const transaction = {
  id: 'transaction-1',
  date: '2026-08-12',
  history: 'Pix Store',
  description: null,
  amount: -45.9,
  category: { id: 'other', name: 'Other' },
  type: 'EXPENSE',
  needsReview: true,
}

const categories = [
  { id: 'other', name: 'Other' },
  { id: 'food', name: 'Food' },
]

function mockApi() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    if (init?.method === 'PATCH') {
      return new Response(JSON.stringify({
        ...transaction,
        category: { id: 'food', name: 'Food' },
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }

    const isCategoriesRequest = String(input).endsWith('/categories')
    return new Response(JSON.stringify(isCategoriesRequest ? categories : [transaction]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
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

  it('sends the transaction category id with learn enabled', async () => {
    const fetchMock = mockApi()

    render(<TransactionTable month="2026-08" />)

    await screen.findByLabelText('Category for Pix Store')
    fireEvent.click(screen.getByRole('button', { name: 'Learn category for Pix Store' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/transactions/transaction-1/category',
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({ categoryId: 'other', learn: true }),
        }),
      )
    })
  })
})
