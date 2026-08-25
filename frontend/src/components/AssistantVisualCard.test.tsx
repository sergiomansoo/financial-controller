import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { AssistantVisualCard } from './AssistantVisualCard'

describe('AssistantVisualCard', () => {
  it('labels a null budget as no limit instead of exceeded', () => {
    render(
      <AssistantVisualCard
        visualData={{
          month: '2026-08',
          categories: [{ name: 'Alimenta\u00e7\u00e3o', spent: 366.36, limit: null, status: 'no_limit' }],
        }}
        visualType="budget_summary"
      />,
    )

    expect(screen.getByText('Sem limite definido')).toBeVisible()
    expect(screen.queryByText(/acima do limite/i)).not.toBeInTheDocument()
  })
})
