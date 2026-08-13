import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { BudgetList } from './BudgetList'

describe('BudgetList', () => {
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
})
