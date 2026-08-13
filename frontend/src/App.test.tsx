import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import App from './App'

describe('App', () => {
  afterEach(() => { cleanup(); localStorage.clear(); vi.unstubAllGlobals() })
  it('renders the finance dashboard shell', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: /financial controller/i }),
    ).toBeInTheDocument()
  })

  it.each(['/dashboard', '/transacoes', '/importar', '/categorias', '/metas', '/configuracoes', '/transactions'])('renders a protected route shell for %s', (path) => {
    localStorage.setItem('financial-controller.session', JSON.stringify({ token: 'token', user: { id: 1, name: 'Ana', email: 'ana@example.com' } }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 })))
    window.history.pushState({}, '', path)
    render(<App />)
    expect(screen.getByRole('main')).toBeInTheDocument()
  })
})
