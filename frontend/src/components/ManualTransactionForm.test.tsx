import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ManualTransactionForm } from './ManualTransactionForm'

describe('ManualTransactionForm', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('submits a numeric amount and selected category', async () => {
    const fetchMock = vi.fn(async (input: string, init?: RequestInit) => {
      if (input.endsWith('/categories')) {
        return new Response(JSON.stringify([{ id: 'food', name: 'Food' }]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      return new Response(JSON.stringify({ id: 'transaction-1' }), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<ManualTransactionForm onCreated={() => undefined} />)

    await screen.findByRole('option', { name: 'Food' })
    fireEvent.change(screen.getByLabelText(/date/i), { target: { value: '2026-08-12' } })
    fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'Lunch' } })
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '45.9' } })
    fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'food' } })
    fireEvent.change(screen.getByLabelText(/type/i), { target: { value: 'EXPENSE' } })
    fireEvent.click(screen.getByRole('button', { name: /add transaction/i }))

    await waitFor(() => {
      const request = fetchMock.mock.calls.find(([url]) => String(url).endsWith('/transactions'))
      expect(request).toBeDefined()
      expect(JSON.parse(String((request?.[1] as RequestInit).body))).toEqual({
        date: '2026-08-12',
        description: 'Lunch',
        amount: 45.9,
        categoryId: 'food',
        type: 'EXPENSE',
      })
    })
  })
})
