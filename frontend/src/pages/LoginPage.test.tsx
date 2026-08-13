import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { AuthProvider } from '../lib/auth'
import { LoginPage } from './LoginPage'

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<h1>Dashboard</h1>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

function fillCredentials() {
  fireEvent.change(screen.getByLabelText(/email/i), {
    target: { value: 'ada@example.com' },
  })
  fireEvent.change(screen.getByLabelText(/senha/i), {
    target: { value: 'correct-horse-battery-staple' },
  })
}

describe('LoginPage', () => {
  afterEach(() => {
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  it('uses ledger auth controls with readable token styles', () => {
    render(<MemoryRouter><AuthProvider><LoginPage /></AuthProvider></MemoryRouter>)
    expect(screen.getByLabelText('Email')).toHaveClass('ledger-auth-control')
    expect(screen.getByRole('main')).toHaveClass('ledger-auth-layout')
  })

  it('shows the server validation message after a failed login', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({ code: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' }),
          { status: 401, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )

    renderLogin()
    fillCredentials()
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Invalid email or password.',
    )
  })

  it('persists the returned session and redirects to the dashboard after login', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            accessToken: 'test-access-token',
            tokenType: 'Bearer',
            user: { id: '1', name: 'Ada Lovelace', email: 'ada@example.com' },
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )

    renderLogin()
    fillCredentials()
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    })
    expect(JSON.parse(localStorage.getItem('financial-controller.session')!)).toEqual({
      token: 'test-access-token',
      user: { id: '1', name: 'Ada Lovelace', email: 'ada@example.com' },
    })
  })
})
