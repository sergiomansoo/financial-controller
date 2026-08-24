import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AppLayout } from './AppLayout'

vi.mock('./MovementFilter', () => ({
  MovementFilter: () => <div data-testid="movement-filter" />,
}))

function mockPhoneViewport() {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: vi.fn().mockImplementation(
    () =>
      ({
        matches: true,
        media: '(max-width: 767px)',
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }) as unknown as MediaQueryList,
    ),
  })
}

describe('AppLayout mobile navigation', () => {
  beforeEach(mockPhoneViewport)

  afterEach(() => {
    document.body.classList.remove('ledger-nav-open')
    vi.unstubAllGlobals()
  })

  it('opens a labelled drawer and closes it with Escape', () => {
    render(
      <MemoryRouter>
        <AppLayout>Conte\u00fado</AppLayout>
      </MemoryRouter>,
    )

    const menuButton = screen.getByRole('button', { name: 'Abrir navega\u00e7\u00e3o' })
    fireEvent.click(menuButton)

    expect(menuButton).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByRole('navigation', { name: 'Navega\u00e7\u00e3o principal' })).toHaveClass(
      'ledger-sidebar--mobile-open',
    )
    expect(screen.getByRole('link', { name: 'Vis\u00e3o geral' })).toBeVisible()
    expect(document.body).toHaveClass('ledger-nav-open')

    fireEvent.keyDown(window, { key: 'Escape' })

    expect(document.body).not.toHaveClass('ledger-nav-open')
    expect(screen.queryByRole('button', { name: 'Fechar menu' })).not.toBeInTheDocument()
    expect(menuButton).toHaveFocus()
  })

  it('closes when the backdrop is activated', () => {
    render(
      <MemoryRouter>
        <AppLayout>Conte\u00fado</AppLayout>
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Abrir navega\u00e7\u00e3o' }))
    fireEvent.click(screen.getByRole('button', { name: 'Fechar menu' }))

    expect(screen.queryByRole('button', { name: 'Fechar menu' })).not.toBeInTheDocument()
    expect(document.body).not.toHaveClass('ledger-nav-open')
  })
})
