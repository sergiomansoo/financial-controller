import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import App from './App'

describe('App', () => {
  afterEach(() => {
    localStorage.clear()
    window.history.pushState({}, '', '/')
  })

  it('renders the finance dashboard shell', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: /financial controller/i }),
    ).toBeInTheDocument()
  })

  it('does not show application navigation on authentication pages', () => {
    window.history.pushState({}, '', '/login')
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Assistente IA' })).not.toBeInTheDocument()
  })

  it('protects and exposes the assistant page from application navigation', () => {
    localStorage.setItem(
      'financial-controller.session',
      JSON.stringify({ token: 'token', user: { id: '1', name: 'Ada', email: 'ada@example.com' } }),
    )
    window.history.pushState({}, '', '/assistant')
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Assistente financeiro' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Assistente IA' })).toHaveAttribute('href', '/assistant')
  })
})
