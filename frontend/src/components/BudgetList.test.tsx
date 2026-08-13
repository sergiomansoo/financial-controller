import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { BudgetList } from './BudgetList'

describe('BudgetList', () => {
  afterEach(() => vi.unstubAllGlobals())
  it('announces an exceeded budget', () => {
    render(
      <BudgetList
        budgets={[
          {
            categoryId: 'food',
            categoryName: 'Food',
            limit: 100,
            spent: 125,
            exceeded: true,
          },
        ]}
        month="2026-08"
        onUpdated={() => undefined}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Food budget exceeded')
  })

  it('sends a numeric valid limit to the budget endpoint', async () => {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(async () => new Response(JSON.stringify({}), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)
    const onUpdated = vi.fn()
    render(<BudgetList budgets={[{ categoryId: 'food', categoryName: 'Food', limit: 100, spent: 50, exceeded: false }]} month="2026-08" onUpdated={onUpdated} />)
    fireEvent.change(screen.getByLabelText('Limit for Food'), { target: { value: '12.34' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save budget' }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/v1\/budgets\/food\?month=2026-08$/)
    expect(JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body))).toEqual({ limit: 12.34 })
    expect(onUpdated).toHaveBeenCalledOnce()
  })

  it.each(['', 'not-a-number', '-1', '1.234'])('does not request an invalid limit of %p', (value) => {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>()
    vi.stubGlobal('fetch', fetchMock)
    render(<BudgetList budgets={[{ categoryId: 'food', categoryName: 'Food', limit: 100, spent: 50, exceeded: false }]} month="2026-08" onUpdated={() => undefined} />)
    fireEvent.change(screen.getByLabelText('Limit for Food'), { target: { value } })
    fireEvent.click(screen.getByRole('button', { name: 'Save budget' }))
    expect(fetchMock).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a non-negative limit with at most two decimal places.')
  })
})
