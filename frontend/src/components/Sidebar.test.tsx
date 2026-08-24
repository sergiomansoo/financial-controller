import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import { Sidebar } from './Sidebar'

describe('Sidebar', () => {
  it('shows a delayed accessible tooltip when collapsed navigation receives focus', async () => {
    render(
      <MemoryRouter>
        <Sidebar collapsed />
      </MemoryRouter>,
    )

    fireEvent.focus(screen.getByRole('link', { name: 'Vis\u00e3o geral' }))

    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('tooltip')).toHaveTextContent('Vis\u00e3o geral'), { timeout: 500 })
  })

  it('notifies the layout after a navigation link is selected from the mobile drawer', () => {
    const onNavigate = vi.fn()

    render(
      <MemoryRouter>
        <Sidebar collapsed={false} isMobileOpen onNavigate={onNavigate} />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('link', { name: 'Transa\u00e7\u00f5es' }))

    expect(onNavigate).toHaveBeenCalledTimes(1)
  })
})
